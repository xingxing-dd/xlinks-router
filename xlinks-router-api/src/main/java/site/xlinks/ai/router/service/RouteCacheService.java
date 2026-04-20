package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Route cache service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCacheService {

    private static final int PROVIDER_FAILURE_THRESHOLD = 3;
    private static final Duration PROVIDER_FAILURE_TTL = Duration.ofMinutes(10);

    private final ModelMapper modelMapper;
    private final PlanMapper planMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderTokenMapper providerTokenMapper;
    private final CustomerTokenMapper customerTokenMapper;
    private final ObjectMapper objectMapper;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private final ConcurrentMap<Long, ProviderFailureMark> providerFailureCache = new ConcurrentHashMap<>();

    private Clock clock = Clock.systemUTC();

    private volatile Map<String, Model> modelCache = Collections.emptyMap();
    private volatile Map<Long, Provider> providerCache = Collections.emptyMap();
    private volatile Map<Long, List<ProviderModel>> providerModelCache = Collections.emptyMap();
    private volatile Map<Long, List<ProviderToken>> providerTokenCache = Collections.emptyMap();
    private volatile Map<Long, ProviderToken> providerTokenByIdCache = Collections.emptyMap();
    private volatile Map<String, CustomerToken> customerTokenByValueCache = Collections.emptyMap();
    private volatile Map<Long, CustomerToken> customerTokenByIdCache = Collections.emptyMap();
    private volatile Map<Long, ProviderProtocolMatcher> providerProtocolCache = Collections.emptyMap();
    private volatile Map<String, List<ProviderModel>> providerModelByModelAndProtocolCache = Collections.emptyMap();
    private volatile Map<Long, PlanModelMatcher> planModelCache = Collections.emptyMap();
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
        withRefreshLock("refreshAll", () -> {
            cleanupExpiredProviderFailures();
            CacheSnapshot snapshot = buildFullSnapshot();
            applySnapshot(snapshot);
            log.info(
                    "Route cache refreshed: models={}, providers={}, modelMappings={}, planRules={}, routingKeys={}, providerTokens={}, customerTokens={}",
                    snapshot.modelCache().size(),
                    snapshot.providerCache().size(),
                    snapshot.providerModelCache().size(),
                    snapshot.planModelCache().size(),
                    snapshot.providerModelByModelAndProtocolCache().size(),
                    snapshot.providerTokenByIdCache().size(),
                    snapshot.customerTokenByValueCache().size()
            );
        });
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

    public void refreshPlans() {
        refreshAll();
    }

    public void refreshCustomerTokens() {
        refreshAll();
    }

    public void refreshModelsOnly() {
        withRefreshLock("refreshModelsOnly", () -> {
            cleanupExpiredProviderFailures();
            List<Model> models = modelMapper.selectList(
                    new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1)
            );
            Map<String, Model> nextModelCache = buildModelCache(models);
            List<Model> nextModelListCache = buildModelList(nextModelCache);
            modelCache = nextModelCache;
            modelListCache = nextModelListCache;
            log.info("Model cache refreshed: models={}", nextModelCache.size());
        });
    }

    public void refreshProvidersOnly() {
        withRefreshLock("refreshProvidersOnly", () -> {
            cleanupExpiredProviderFailures();
            List<Provider> providers = providerMapper.selectList(
                    new LambdaQueryWrapper<Provider>().eq(Provider::getStatus, 1)
            );
            Map<Long, Provider> nextProviderCache = buildProviderCache(providers);
            Map<Long, ProviderProtocolMatcher> nextProviderProtocolCache = buildProviderProtocolCache(providers);
            Map<String, List<ProviderModel>> nextRoutingIndex = buildRoutingIndex(
                    providerModelCache,
                    nextProviderCache,
                    nextProviderProtocolCache
            );
            providerCache = nextProviderCache;
            providerProtocolCache = nextProviderProtocolCache;
            providerModelByModelAndProtocolCache = nextRoutingIndex;
            log.info("Provider cache refreshed: providers={}, routingKeys={}",
                    nextProviderCache.size(), nextRoutingIndex.size());
        });
    }

    public void refreshProviderModelsOnly() {
        withRefreshLock("refreshProviderModelsOnly", () -> {
            cleanupExpiredProviderFailures();
            List<ProviderModel> providerModels = providerModelMapper.selectList(
                    new LambdaQueryWrapper<ProviderModel>().eq(ProviderModel::getStatus, 1)
            );
            Map<Long, List<ProviderModel>> nextProviderModelCache = buildProviderModelCache(providerModels);
            Map<String, List<ProviderModel>> nextRoutingIndex = buildRoutingIndex(
                    nextProviderModelCache,
                    providerCache,
                    providerProtocolCache
            );
            providerModelCache = nextProviderModelCache;
            providerModelByModelAndProtocolCache = nextRoutingIndex;
            log.info("Provider model cache refreshed: modelMappings={}, routingKeys={}",
                    nextProviderModelCache.size(), nextRoutingIndex.size());
        });
    }

    public void refreshProviderTokensOnly() {
        withRefreshLock("refreshProviderTokensOnly", () -> {
            cleanupExpiredProviderFailures();
            List<ProviderToken> providerTokens = providerTokenMapper.selectList(
                    new LambdaQueryWrapper<ProviderToken>().eq(ProviderToken::getTokenStatus, 1)
            );
            Map<Long, List<ProviderToken>> nextProviderTokenCache = buildProviderTokenCache(providerTokens);
            Map<Long, ProviderToken> nextProviderTokenByIdCache = buildProviderTokenByIdCache(providerTokens);
            providerTokenCache = nextProviderTokenCache;
            providerTokenByIdCache = nextProviderTokenByIdCache;
            log.info("Provider token cache refreshed: providers={}, tokens={}",
                    nextProviderTokenCache.size(), nextProviderTokenByIdCache.size());
        });
    }

    public void refreshPlansOnly() {
        withRefreshLock("refreshPlansOnly", () -> {
            cleanupExpiredProviderFailures();
            List<Plan> plans = planMapper.selectList(
                    new LambdaQueryWrapper<Plan>().select(Plan::getId, Plan::getAllowedModels)
            );
            Map<Long, PlanModelMatcher> nextPlanModelCache = buildPlanModelCache(plans);
            planModelCache = nextPlanModelCache;
            log.info("Plan cache refreshed: planRules={}", nextPlanModelCache.size());
        });
    }

    public void refreshCustomerTokensByAccountId(Long accountId) {
        if (accountId == null) {
            return;
        }
        withRefreshLock("refreshCustomerTokensByAccountId", () -> {
            cleanupExpiredProviderFailures();
            List<CustomerToken> latestTokens = customerTokenMapper.selectList(
                    new LambdaQueryWrapper<CustomerToken>().eq(CustomerToken::getAccountId, accountId)
            );

            Map<String, CustomerToken> nextByValue = new HashMap<>(customerTokenByValueCache);
            nextByValue.entrySet().removeIf(entry -> {
                CustomerToken token = entry.getValue();
                return token != null && accountId.equals(token.getAccountId());
            });

            Map<Long, CustomerToken> nextById = new HashMap<>(customerTokenByIdCache);
            nextById.entrySet().removeIf(entry -> {
                CustomerToken token = entry.getValue();
                return token != null && accountId.equals(token.getAccountId());
            });

            for (CustomerToken token : latestTokens) {
                if (token == null) {
                    continue;
                }
                if (token.getTokenValue() != null && !token.getTokenValue().isBlank()) {
                    nextByValue.put(token.getTokenValue(), token);
                }
                if (token.getId() != null) {
                    nextById.put(token.getId(), token);
                }
            }

            customerTokenByValueCache = Collections.unmodifiableMap(nextByValue);
            customerTokenByIdCache = Collections.unmodifiableMap(nextById);
            log.info("Customer token cache refreshed incrementally: accountId={}, loadedTokens={}",
                    accountId, latestTokens.size());
        });
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

    public ProviderModel selectProviderModel(Long modelId, ProxyProtocol protocol) {
        return listProviderModelsByPriority(modelId, protocol).stream()
                .findFirst()
                .orElse(null);
    }

    public boolean isModelSupportedByPlan(Long planId, String modelCode) {
        if (planId == null || modelCode == null || modelCode.isBlank()) {
            return false;
        }
        PlanModelMatcher matcher = planModelCache.get(planId);
        if (matcher == null) {
            return false;
        }
        return matcher.matches(modelCode);
    }

    public List<ProviderModel> listProviderModelsByPriority(Long modelId, ProxyProtocol protocol) {
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

    public CustomerToken getCustomerTokenByValue(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return null;
        }
        CustomerToken cached = customerTokenByValueCache.get(tokenValue);
        if (cached != null) {
            return cached;
        }

        CustomerToken customerToken = customerTokenMapper.selectOne(
                new LambdaQueryWrapper<CustomerToken>().eq(CustomerToken::getTokenValue, tokenValue)
        );
        if (customerToken != null) {
            cacheCustomerToken(customerToken);
        }
        return customerToken;
    }

    public void cacheCustomerToken(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getTokenValue() == null || customerToken.getTokenValue().isBlank()) {
            return;
        }
        Map<String, CustomerToken> nextByValue = new HashMap<>(customerTokenByValueCache);
        nextByValue.put(customerToken.getTokenValue(), customerToken);

        Map<Long, CustomerToken> nextById = new HashMap<>(customerTokenByIdCache);
        if (customerToken.getId() != null) {
            nextById.put(customerToken.getId(), customerToken);
        }

        customerTokenByValueCache = Collections.unmodifiableMap(nextByValue);
        customerTokenByIdCache = Collections.unmodifiableMap(nextById);
    }

    public void updateCustomerTokenQuota(Long tokenId, BigDecimal usedQuota, BigDecimal totalUsedQuota) {
        if (tokenId == null) {
            return;
        }
        CustomerToken token = customerTokenByIdCache.get(tokenId);
        if (token == null) {
            return;
        }
        token.setUsedQuota(usedQuota);
        token.setTotalUsedQuota(totalUsedQuota);
    }

    public void recordProviderFailure(Long providerId) {
        if (providerId == null) {
            return;
        }
        Instant now = Instant.now(clock);
        ProviderFailureMark mark = providerFailureCache.compute(providerId, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new ProviderFailureMark(1, now);
            }
            return new ProviderFailureMark(existing.failureCount() + 1, existing.firstFailureAt());
        });
        if (mark != null && mark.failureCount() > PROVIDER_FAILURE_THRESHOLD) {
            log.warn("Provider marked temporarily unavailable after consecutive failures. providerId={}, failureCount={}, firstFailureAt={}",
                    providerId, mark.failureCount(), mark.firstFailureAt());
        } else if (mark != null) {
            log.info("Provider failure recorded. providerId={}, failureCount={}, firstFailureAt={}",
                    providerId, mark.failureCount(), mark.firstFailureAt());
        }
    }

    public void clearProviderFailure(Long providerId) {
        if (providerId == null) {
            return;
        }
        ProviderFailureMark removed = providerFailureCache.remove(providerId);
        if (removed != null) {
            log.info("Provider failure state cleared after successful response. providerId={}, clearedFailureCount={}",
                    providerId, removed.failureCount());
        }
    }

    public boolean isProviderTemporarilyUnavailable(Long providerId) {
        if (providerId == null) {
            return false;
        }
        ProviderFailureMark mark = providerFailureCache.get(providerId);
        if (mark == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        if (mark.isExpired(now)) {
            providerFailureCache.remove(providerId, mark);
            log.info("Expired provider failure state cleaned on read. providerId={}", providerId);
            return false;
        }
        return mark.failureCount() > PROVIDER_FAILURE_THRESHOLD;
    }

    private CacheSnapshot buildFullSnapshot() {
        List<Provider> providers = providerMapper.selectList(
                new LambdaQueryWrapper<Provider>().eq(Provider::getStatus, 1)
        );
        List<Model> models = modelMapper.selectList(
                new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1)
        );
        List<Plan> plans = planMapper.selectList(
                new LambdaQueryWrapper<Plan>().select(Plan::getId, Plan::getAllowedModels)
        );
        List<ProviderModel> providerModels = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModel>().eq(ProviderModel::getStatus, 1)
        );
        List<ProviderToken> providerTokens = providerTokenMapper.selectList(
                new LambdaQueryWrapper<ProviderToken>().eq(ProviderToken::getTokenStatus, 1)
        );
        List<CustomerToken> customerTokens = customerTokenMapper.selectList(new LambdaQueryWrapper<>());

        Map<String, Model> nextModelCache = buildModelCache(models);
        Map<Long, Provider> nextProviderCache = buildProviderCache(providers);
        Map<Long, ProviderProtocolMatcher> nextProviderProtocolCache = buildProviderProtocolCache(providers);
        Map<Long, List<ProviderModel>> nextProviderModelCache = buildProviderModelCache(providerModels);
        Map<Long, List<ProviderToken>> nextProviderTokenCache = buildProviderTokenCache(providerTokens);
        Map<Long, ProviderToken> nextProviderTokenByIdCache = buildProviderTokenByIdCache(providerTokens);
        Map<Long, PlanModelMatcher> nextPlanModelCache = buildPlanModelCache(plans);
        Map<String, List<ProviderModel>> nextRoutingIndex = buildRoutingIndex(
                nextProviderModelCache,
                nextProviderCache,
                nextProviderProtocolCache
        );
        Map<String, CustomerToken> nextCustomerTokenByValueCache = buildCustomerTokenByValueCache(customerTokens);
        Map<Long, CustomerToken> nextCustomerTokenByIdCache = buildCustomerTokenByIdCache(customerTokens);

        return new CacheSnapshot(
                nextModelCache,
                buildModelList(nextModelCache),
                nextProviderCache,
                nextProviderModelCache,
                nextProviderTokenCache,
                nextProviderTokenByIdCache,
                nextCustomerTokenByValueCache,
                nextCustomerTokenByIdCache,
                nextProviderProtocolCache,
                nextRoutingIndex,
                nextPlanModelCache
        );
    }

    private void applySnapshot(CacheSnapshot snapshot) {
        modelCache = snapshot.modelCache();
        modelListCache = snapshot.modelListCache();
        providerCache = snapshot.providerCache();
        providerModelCache = snapshot.providerModelCache();
        providerTokenCache = snapshot.providerTokenCache();
        providerTokenByIdCache = snapshot.providerTokenByIdCache();
        customerTokenByValueCache = snapshot.customerTokenByValueCache();
        customerTokenByIdCache = snapshot.customerTokenByIdCache();
        providerProtocolCache = snapshot.providerProtocolCache();
        providerModelByModelAndProtocolCache = snapshot.providerModelByModelAndProtocolCache();
        planModelCache = snapshot.planModelCache();
    }

    private void withRefreshLock(String operation, Runnable runnable) {
        refreshLock.lock();
        try {
            runnable.run();
        } finally {
            refreshLock.unlock();
            log.debug("Cache refresh operation finished: {}", operation);
        }
    }

    private void cleanupExpiredProviderFailures() {
        Instant now = Instant.now(clock);
        providerFailureCache.entrySet().removeIf(entry -> {
            ProviderFailureMark mark = entry.getValue();
            return mark == null || mark.isExpired(now);
        });
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

    private Map<Long, PlanModelMatcher> buildPlanModelCache(List<Plan> plans) {
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, PlanModelMatcher> map = new HashMap<>();
        for (Plan plan : plans) {
            if (plan == null || plan.getId() == null) {
                continue;
            }
            map.put(plan.getId(), parsePlanModelMatcher(plan.getAllowedModels()));
        }
        return Collections.unmodifiableMap(map);
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

            for (ProxyProtocol protocol : ProxyProtocol.values()) {
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

    private Map<String, CustomerToken> buildCustomerTokenByValueCache(List<CustomerToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, CustomerToken> map = new HashMap<>();
        for (CustomerToken token : tokens) {
            if (token == null || token.getTokenValue() == null || token.getTokenValue().isBlank()) {
                continue;
            }
            map.put(token.getTokenValue(), token);
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, CustomerToken> buildCustomerTokenByIdCache(List<CustomerToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CustomerToken> map = new HashMap<>();
        for (CustomerToken token : tokens) {
            if (token == null || token.getId() == null) {
                continue;
            }
            map.put(token.getId(), token);
        }
        return Collections.unmodifiableMap(map);
    }

    private PlanModelMatcher parsePlanModelMatcher(String rawAllowedModels) {
        if (rawAllowedModels == null || rawAllowedModels.isBlank()) {
            return new PlanModelMatcher(true, Collections.emptySet());
        }
        String trimmed = rawAllowedModels.trim();
        try {
            Set<String> values;
            if (trimmed.startsWith("[")) {
                List<String> models = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {
                });
                values = models == null
                        ? Collections.emptySet()
                        : models.stream()
                        .filter(item -> item != null && !item.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet());
            } else {
                values = Arrays.stream(trimmed.split("[,;\uFF0C]"))
                        .map(item -> item == null ? "" : item.trim())
                        .filter(item -> !item.isBlank())
                        .collect(Collectors.toSet());
            }
            if (values.isEmpty()) {
                return new PlanModelMatcher(true, Collections.emptySet());
            }
            return new PlanModelMatcher(false, Collections.unmodifiableSet(values));
        } catch (Exception ex) {
            log.warn("Failed to parse plan allowed models: {}", rawAllowedModels, ex);
            return new PlanModelMatcher(true, Collections.emptySet());
        }
    }

    private String buildRoutingIndexKey(Long modelId, ProxyProtocol protocol) {
        return modelId + "#" + protocol.getCode();
    }

    private boolean supportsProtocol(Provider provider, ProxyProtocol protocol) {
        if (provider == null) {
            return false;
        }
        ProviderProtocolMatcher matcher = providerProtocolCache.get(provider.getId());
        return matcher == null || matcher.matches(protocol);
    }

    private int resolvePriority(Provider provider) {
        return provider == null || provider.getPriority() == null ? 0 : provider.getPriority();
    }

    private record CacheSnapshot(Map<String, Model> modelCache,
                                 List<Model> modelListCache,
                                 Map<Long, Provider> providerCache,
                                 Map<Long, List<ProviderModel>> providerModelCache,
                                 Map<Long, List<ProviderToken>> providerTokenCache,
                                 Map<Long, ProviderToken> providerTokenByIdCache,
                                 Map<String, CustomerToken> customerTokenByValueCache,
                                 Map<Long, CustomerToken> customerTokenByIdCache,
                                 Map<Long, ProviderProtocolMatcher> providerProtocolCache,
                                 Map<String, List<ProviderModel>> providerModelByModelAndProtocolCache,
                                 Map<Long, PlanModelMatcher> planModelCache) {
    }

    private record PlanModelMatcher(boolean allowAll, Set<String> allowedModels) {
        boolean matches(String modelCode) {
            if (modelCode == null || modelCode.isBlank()) {
                return false;
            }
            if (allowAll) {
                return true;
            }
            if (allowedModels == null || allowedModels.isEmpty()) {
                return true;
            }
            return allowedModels.contains(modelCode.trim());
        }
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

        boolean matches(ProxyProtocol protocol) {
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

    private record ProviderFailureMark(int failureCount, Instant firstFailureAt) {
        boolean isExpired(Instant now) {
            if (firstFailureAt == null || now == null) {
                return true;
            }
            return now.isAfter(firstFailureAt.plus(PROVIDER_FAILURE_TTL));
        }
    }
}
