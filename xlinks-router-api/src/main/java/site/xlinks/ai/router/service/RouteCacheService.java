package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.OpenAIProtocol;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Route cache service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCacheService {

    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderTokenMapper providerTokenMapper;

    private volatile Map<String, Model> modelCache = Collections.emptyMap();
    private volatile Map<Long, Provider> providerCache = Collections.emptyMap();
    private volatile Map<Long, List<ProviderModel>> providerModelCache = Collections.emptyMap();
    private volatile Map<Long, List<ProviderToken>> providerTokenCache = Collections.emptyMap();
    private volatile Map<Long, ProviderToken> providerTokenByIdCache = Collections.emptyMap();
    private volatile Map<Long, ProviderProtocolMatcher> providerProtocolCache = Collections.emptyMap();
    private volatile Map<String, List<ProviderModel>> providerModelByModelAndProtocolCache = Collections.emptyMap();
    private volatile List<Model> modelListCache = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshAll();
    }

    @Scheduled(fixedDelayString = "${xlinks.router.cache.refresh-interval-ms:300000}")
    public void scheduledRefresh() {
        refreshAll();
    }

    public void refreshAll() {
        List<Provider> providers = providerMapper.selectList(
                new LambdaQueryWrapper<Provider>().eq(Provider::getStatus, 1)
        );
        List<Model> models = modelMapper.selectList(
                new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1)
        );
        List<ProviderModel> providerModels = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModel>().eq(ProviderModel::getStatus, 1)
        );
        List<ProviderToken> providerTokens = providerTokenMapper.selectList(
                new LambdaQueryWrapper<ProviderToken>().eq(ProviderToken::getTokenStatus, 1)
        );

        Map<String, Model> nextModelCache = buildModelCache(models);
        Map<Long, Provider> nextProviderCache = buildProviderCache(providers);
        Map<Long, ProviderProtocolMatcher> nextProtocolCache = buildProviderProtocolCache(providers);
        Map<Long, List<ProviderModel>> nextProviderModelCache = buildProviderModelCache(providerModels);
        Map<String, List<ProviderModel>> nextRoutingIndex = buildRoutingIndex(
                nextProviderModelCache,
                nextProviderCache,
                nextProtocolCache
        );
        Map<Long, List<ProviderToken>> nextProviderTokenCache = buildProviderTokenCache(providerTokens);
        Map<Long, ProviderToken> nextProviderTokenByIdCache = buildProviderTokenByIdCache(providerTokens);

        // Atomic-ish snapshot swap via volatile references.
        modelCache = nextModelCache;
        modelListCache = buildModelList(nextModelCache);
        providerCache = nextProviderCache;
        providerProtocolCache = nextProtocolCache;
        providerModelCache = nextProviderModelCache;
        providerModelByModelAndProtocolCache = nextRoutingIndex;
        providerTokenCache = nextProviderTokenCache;
        providerTokenByIdCache = nextProviderTokenByIdCache;

        log.info(
                "Route cache refreshed: models={}, providers={}, modelMappings={}, routingKeys={}, providerTokens={}",
                modelCache.size(),
                providerCache.size(),
                providerModelCache.size(),
                providerModelByModelAndProtocolCache.size(),
                providerTokenByIdCache.size()
        );
    }

    public void refreshModels() {
        refreshAll();
    }

    public void refreshProviders() {
        refreshAll();
    }

    public void refreshProviderModels() {
        refreshAll();
    }

    public void refreshProviderTokens() {
        refreshAll();
    }

    public List<Model> listModels() {
        return modelListCache;
    }

    public Model getModel(String modelCode) {
        Model cached = modelCache.get(modelCode);
        if (cached != null) {
            return cached;
        }
        Model model = modelMapper.selectOne(
                new LambdaQueryWrapper<Model>()
                        .eq(Model::getModelCode, modelCode)
                        .eq(Model::getStatus, 1)
        );
        if (model != null && model.getModelCode() != null) {
            Map<String, Model> copy = new HashMap<>(modelCache);
            copy.put(model.getModelCode(), model);
            modelCache = Collections.unmodifiableMap(copy);
            modelListCache = buildModelList(copy);
        }
        return model;
    }

    public ProviderModel selectProviderModel(Long modelId, OpenAIProtocol protocol) {
        return listProviderModelsByPriority(modelId, protocol).stream()
                .findFirst()
                .orElse(null);
    }

    public List<ProviderModel> listProviderModelsByPriority(Long modelId, OpenAIProtocol protocol) {
        if (modelId == null || protocol == null) {
            return Collections.emptyList();
        }

        List<ProviderModel> indexed = providerModelByModelAndProtocolCache.get(buildRoutingIndexKey(modelId, protocol));
        if (indexed != null) {
            return indexed;
        }

        List<ProviderModel> candidates = getProviderModels(modelId);
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        return candidates.stream()
                .filter(mapping -> mapping.getProviderId() != null)
                .filter(mapping -> supportsProtocol(getProvider(mapping.getProviderId()), protocol))
                .sorted(Comparator
                        .comparingInt((ProviderModel mapping) -> resolvePriority(getProvider(mapping.getProviderId())))
                        .reversed()
                        .thenComparing(mapping -> mapping.getId() == null ? Long.MAX_VALUE : mapping.getId()))
                .toList();
    }

    public List<ProviderModel> getProviderModels(Long modelId) {
        List<ProviderModel> cached = providerModelCache.get(modelId);
        if (cached != null) {
            return cached;
        }
        List<ProviderModel> providerModels = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModel>()
                        .eq(ProviderModel::getModelId, modelId)
                        .eq(ProviderModel::getStatus, 1)
        );
        if (providerModels == null || providerModels.isEmpty()) {
            Map<Long, List<ProviderModel>> copy = new HashMap<>(providerModelCache);
            copy.put(modelId, Collections.emptyList());
            providerModelCache = Collections.unmodifiableMap(copy);
            return Collections.emptyList();
        }

        List<ProviderModel> immutable = List.copyOf(providerModels);
        Map<Long, List<ProviderModel>> copy = new HashMap<>(providerModelCache);
        copy.put(modelId, immutable);
        providerModelCache = Collections.unmodifiableMap(copy);
        return immutable;
    }

    public Provider getProvider(Long providerId) {
        Provider cached = providerCache.get(providerId);
        if (cached != null) {
            return cached;
        }
        Provider provider = providerMapper.selectById(providerId);
        if (provider == null || provider.getStatus() == null || provider.getStatus() != 1) {
            return null;
        }
        Map<Long, Provider> copy = new HashMap<>(providerCache);
        copy.put(providerId, provider);
        providerCache = Collections.unmodifiableMap(copy);

        Map<Long, ProviderProtocolMatcher> protocolCopy = new HashMap<>(providerProtocolCache);
        protocolCopy.put(providerId, ProviderProtocolMatcher.parse(provider.getSupportedProtocols()));
        providerProtocolCache = Collections.unmodifiableMap(protocolCopy);
        return provider;
    }

    public List<ProviderToken> getProviderTokens(Long providerId) {
        List<ProviderToken> cached = providerTokenCache.get(providerId);
        if (cached != null) {
            return cached;
        }

        List<ProviderToken> tokens = providerTokenMapper.selectList(
                new LambdaQueryWrapper<ProviderToken>()
                        .eq(ProviderToken::getProviderId, providerId)
                        .eq(ProviderToken::getTokenStatus, 1)
        );
        if (tokens == null || tokens.isEmpty()) {
            Map<Long, List<ProviderToken>> copy = new HashMap<>(providerTokenCache);
            copy.put(providerId, Collections.emptyList());
            providerTokenCache = Collections.unmodifiableMap(copy);
            return Collections.emptyList();
        }

        List<ProviderToken> immutable = List.copyOf(tokens);
        Map<Long, List<ProviderToken>> tokenCopy = new HashMap<>(providerTokenCache);
        tokenCopy.put(providerId, immutable);
        providerTokenCache = Collections.unmodifiableMap(tokenCopy);

        Map<Long, ProviderToken> byIdCopy = new HashMap<>(providerTokenByIdCache);
        for (ProviderToken token : tokens) {
            if (token.getId() != null) {
                byIdCopy.put(token.getId(), token);
            }
        }
        providerTokenByIdCache = Collections.unmodifiableMap(byIdCopy);
        return immutable;
    }

    public void touchProviderToken(Long tokenId, LocalDateTime lastUsedAt) {
        if (tokenId == null) {
            return;
        }
        ProviderToken token = providerTokenByIdCache.get(tokenId);
        if (token != null) {
            token.setLastUsedAt(lastUsedAt);
        }
    }

    public void updateProviderTokenQuota(Long tokenId, Long quotaUsed) {
        if (tokenId == null) {
            return;
        }
        ProviderToken token = providerTokenByIdCache.get(tokenId);
        if (token != null) {
            token.setQuotaUsed(quotaUsed);
        }
    }

    private Map<String, Model> buildModelCache(List<Model> models) {
        if (models == null || models.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Model> map = new HashMap<>();
        for (Model model : models) {
            if (model.getModelCode() == null || model.getModelCode().isBlank()) {
                continue;
            }
            map.put(model.getModelCode(), model);
        }
        return Collections.unmodifiableMap(map);
    }

    private List<Model> buildModelList(Map<String, Model> modelMap) {
        if (modelMap == null || modelMap.isEmpty()) {
            return Collections.emptyList();
        }
        return modelMap.values().stream()
                .sorted(Comparator.comparing(model -> model.getModelCode() == null ? "" : model.getModelCode()))
                .toList();
    }

    private Map<Long, Provider> buildProviderCache(List<Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Provider> map = new HashMap<>();
        for (Provider provider : providers) {
            if (provider.getId() == null) {
                continue;
            }
            map.put(provider.getId(), provider);
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, ProviderProtocolMatcher> buildProviderProtocolCache(List<Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ProviderProtocolMatcher> map = new HashMap<>();
        for (Provider provider : providers) {
            if (provider.getId() == null) {
                continue;
            }
            map.put(provider.getId(), ProviderProtocolMatcher.parse(provider.getSupportedProtocols()));
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, List<ProviderModel>> buildProviderModelCache(List<ProviderModel> providerModels) {
        if (providerModels == null || providerModels.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ProviderModel>> grouped = new HashMap<>();
        for (ProviderModel providerModel : providerModels) {
            if (providerModel.getModelId() == null) {
                continue;
            }
            grouped.computeIfAbsent(providerModel.getModelId(), ignored -> new ArrayList<>()).add(providerModel);
        }
        Map<Long, List<ProviderModel>> immutable = new HashMap<>();
        for (Map.Entry<Long, List<ProviderModel>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    private Map<String, List<ProviderModel>> buildRoutingIndex(Map<Long, List<ProviderModel>> providerModelsByModel,
                                                               Map<Long, Provider> providers,
                                                               Map<Long, ProviderProtocolMatcher> protocolMatchers) {
        if (providerModelsByModel == null || providerModelsByModel.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<ProviderModel>> index = new HashMap<>();
        for (Map.Entry<Long, List<ProviderModel>> entry : providerModelsByModel.entrySet()) {
            Long modelId = entry.getKey();
            List<ProviderModel> mappings = entry.getValue();
            if (mappings == null || mappings.isEmpty()) {
                continue;
            }

            for (OpenAIProtocol protocol : OpenAIProtocol.values()) {
                List<ProviderModel> sorted = mappings.stream()
                        .filter(mapping -> mapping.getProviderId() != null)
                        .filter(mapping -> {
                            Provider provider = providers.get(mapping.getProviderId());
                            if (provider == null) {
                                return false;
                            }
                            ProviderProtocolMatcher matcher = protocolMatchers.get(provider.getId());
                            return matcher == null || matcher.matches(protocol);
                        })
                        .sorted(Comparator
                                .comparingInt((ProviderModel mapping) -> resolvePriority(providers.get(mapping.getProviderId())))
                                .reversed()
                                .thenComparing(mapping -> mapping.getId() == null ? Long.MAX_VALUE : mapping.getId()))
                        .toList();
                index.put(buildRoutingIndexKey(modelId, protocol), List.copyOf(sorted));
            }
        }
        return Collections.unmodifiableMap(index);
    }

    private Map<Long, List<ProviderToken>> buildProviderTokenCache(List<ProviderToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ProviderToken>> grouped = new HashMap<>();
        for (ProviderToken token : tokens) {
            if (token.getProviderId() == null) {
                continue;
            }
            grouped.computeIfAbsent(token.getProviderId(), ignored -> new ArrayList<>()).add(token);
        }
        Map<Long, List<ProviderToken>> immutable = new HashMap<>();
        for (Map.Entry<Long, List<ProviderToken>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    private Map<Long, ProviderToken> buildProviderTokenByIdCache(List<ProviderToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ProviderToken> map = new HashMap<>();
        for (ProviderToken token : tokens) {
            if (token.getId() != null) {
                map.put(token.getId(), token);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private String buildRoutingIndexKey(Long modelId, OpenAIProtocol protocol) {
        return modelId + "#" + protocol.getCode();
    }

    private boolean supportsProtocol(Provider provider, OpenAIProtocol protocol) {
        if (provider == null) {
            return false;
        }
        ProviderProtocolMatcher matcher = providerProtocolCache.get(provider.getId());
        return matcher == null || matcher.matches(protocol);
    }

    private int resolvePriority(Provider provider) {
        return provider == null || provider.getPriority() == null ? 0 : provider.getPriority();
    }

    private record ProviderProtocolMatcher(boolean allowAll, Set<String> normalizedProtocols) {
        static ProviderProtocolMatcher parse(String rawSupportedProtocols) {
            if (rawSupportedProtocols == null || rawSupportedProtocols.isBlank()) {
                return new ProviderProtocolMatcher(true, Collections.emptySet());
            }
            Set<String> values = Arrays.stream(rawSupportedProtocols.split("[,;\uFF0C]"))
                    .map(item -> item == null ? "" : item.trim())
                    .filter(item -> !item.isBlank())
                    .map(ProviderProtocolMatcher::normalize)
                    .collect(Collectors.toSet());
            if (values.isEmpty() || values.contains("*")) {
                return new ProviderProtocolMatcher(true, Collections.emptySet());
            }
            return new ProviderProtocolMatcher(false, values);
        }

        boolean matches(OpenAIProtocol protocol) {
            if (protocol == null) {
                return false;
            }
            if (allowAll) {
                return true;
            }
            if (normalizedProtocols == null || normalizedProtocols.isEmpty()) {
                return true;
            }
            return normalizedProtocols.contains(normalize(protocol.getCode()))
                    || normalizedProtocols.contains(normalize(protocol.getProviderPath()))
                    || normalizedProtocols.contains(normalize(protocol.name()));
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }
    }
}
