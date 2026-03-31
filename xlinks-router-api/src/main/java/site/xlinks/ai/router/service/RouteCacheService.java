package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.OpenAIProtocol;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ModelEndpointMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Route cache service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCacheService {

    private final ModelEndpointMapper modelEndpointMapper;
    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;
    private final ProviderTokenMapper providerTokenMapper;

    private final Map<String, ModelEndpoint> endpointCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, List<Model>>> modelCache = new ConcurrentHashMap<>();
    private final Map<Long, Provider> providerCache = new ConcurrentHashMap<>();
    private final Map<Long, List<ProviderToken>> providerTokenCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshAll();
    }

    @Scheduled(fixedDelayString = "${xlinks.router.cache.refresh-interval-ms:300000}")
    public void scheduledRefresh() {
        refreshAll();
    }

    public void refreshAll() {
        refreshProviders();
        refreshEndpoints();
        refreshModels();
        refreshProviderTokens();
        log.info("Route cache refreshed");
    }

    public void refreshEndpoints() {
        List<ModelEndpoint> endpoints = modelEndpointMapper.selectList(
                new LambdaQueryWrapper<ModelEndpoint>().eq(ModelEndpoint::getStatus, 1)
        );
        endpointCache.clear();
        for (ModelEndpoint endpoint : endpoints) {
            endpointCache.put(endpoint.getEndpointCode(), endpoint);
        }
        log.debug("Endpoint cache loaded, size={}", endpointCache.size());
    }

    public void refreshModels() {
        List<Model> models = modelMapper.selectList(
                new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1)
        );
        modelCache.clear();
        for (Model model : models) {
            if (model.getEndpointId() == null || model.getModelCode() == null) {
                continue;
            }
            modelCache
                    .computeIfAbsent(model.getEndpointId(), id -> new ConcurrentHashMap<>())
                    .computeIfAbsent(model.getModelCode(), key -> new ArrayList<>())
                    .add(model);
        }
        log.debug("Model cache loaded, endpointCount={}", modelCache.size());
    }

    public void refreshProviders() {
        List<Provider> providers = providerMapper.selectList(
                new LambdaQueryWrapper<Provider>().eq(Provider::getStatus, 1)
        );
        providerCache.clear();
        for (Provider provider : providers) {
            providerCache.put(provider.getId(), provider);
        }
        log.debug("Provider cache loaded, size={}", providerCache.size());
    }

    public void refreshProviderTokens() {
        List<ProviderToken> tokens = providerTokenMapper.selectList(
                new LambdaQueryWrapper<ProviderToken>().eq(ProviderToken::getTokenStatus, 1)
        );
        providerTokenCache.clear();
        for (ProviderToken token : tokens) {
            if (token.getProviderId() == null) {
                continue;
            }
            providerTokenCache
                    .computeIfAbsent(token.getProviderId(), id -> new ArrayList<>())
                    .add(token);
        }
        log.debug("Provider token cache loaded, providerCount={}", providerTokenCache.size());
    }

    public ModelEndpoint getEndpoint(String endpointCode) {
        ModelEndpoint cached = endpointCache.get(endpointCode);
        if (cached != null) {
            return cached;
        }
        ModelEndpoint endpoint = modelEndpointMapper.selectOne(
                new LambdaQueryWrapper<ModelEndpoint>()
                        .eq(ModelEndpoint::getEndpointCode, endpointCode)
                        .eq(ModelEndpoint::getStatus, 1)
        );
        if (endpoint != null) {
            endpointCache.put(endpointCode, endpoint);
        }
        return endpoint;
    }

    public Provider selectProvider(Long endpointId, String modelCode, OpenAIProtocol protocol) {
        return this.providerCache.values().stream()
                .filter(provider -> supportsProtocol(provider, protocol)).min(Comparator
                        .comparingInt(this::resolvePriority)
                        .reversed()
                        .thenComparing(provider -> provider.getId() == null ? Long.MAX_VALUE : provider.getId()))
                .orElse(null);
    }

    public Model selectModel(Long endpointId, String modelCode, OpenAIProtocol protocol) {
        Provider provider = selectProvider(endpointId, modelCode, protocol);
        if (provider == null) {
            return null;
        }
        return getModel(endpointId, modelCode, provider.getId());
    }

    public Model getModel(Long endpointId, String modelCode, Long providerId) {
        List<Model> candidates = getModels(endpointId, modelCode);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator.comparing(model -> model.getId() == null ? Long.MAX_VALUE : model.getId()))
                .orElse(null);
    }

    public List<Model> getModels(Long endpointId, String modelCode) {
        Map<String, List<Model>> endpointModels = modelCache.get(endpointId);
        if (endpointModels != null) {
            List<Model> cached = endpointModels.get(modelCode);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        }
        List<Model> models = modelMapper.selectList(
                new LambdaQueryWrapper<Model>()
                        .eq(Model::getModelCode, modelCode)
                        .eq(Model::getEndpointId, endpointId)
                        .eq(Model::getStatus, 1)
        );
        if (models != null && !models.isEmpty()) {
            List<Model> cachedModels = new ArrayList<>(models);
            modelCache
                    .computeIfAbsent(endpointId, id -> new ConcurrentHashMap<>())
                    .put(modelCode, cachedModels);
            return cachedModels;
        }
        return models;
    }

    public Provider getProvider(Long providerId) {
        Provider cached = providerCache.get(providerId);
        if (cached != null) {
            return cached;
        }
        Provider provider = providerMapper.selectById(providerId);
        if (provider != null && provider.getStatus() != null && provider.getStatus() == 1) {
            providerCache.put(providerId, provider);
        }
        return provider;
    }

    public List<Provider> getProvidersByModel(Long endpointId, String modelCode) {
        List<Model> candidates = getModels(endpointId, modelCode);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(Model::getProviderId)
                .filter(providerId -> providerId != null)
                .distinct()
                .map(this::getProvider)
                .filter(provider -> provider != null)
                .collect(Collectors.toList());
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
        if (tokens != null && !tokens.isEmpty()) {
            providerTokenCache.put(providerId, tokens);
        }
        return tokens;
    }

    private boolean supportsProtocol(Provider provider, OpenAIProtocol protocol) {
        if (provider == null) {
            return false;
        }
        String supportedProtocols = provider.getSupportedProtocols();
        if (supportedProtocols == null || supportedProtocols.isBlank()) {
            return true;
        }
        String[] items = supportedProtocols.split("[,;?]");
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String value = item.trim();
            if ("*".equals(value) || protocol.matches(value)) {
                return true;
            }
        }
        return false;
    }

    private int resolvePriority(Provider provider) {
        return provider == null || provider.getPriority() == null ? 0 : provider.getPriority();
    }
}
