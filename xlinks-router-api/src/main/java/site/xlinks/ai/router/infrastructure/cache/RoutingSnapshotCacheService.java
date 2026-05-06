package site.xlinks.ai.router.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderProtocolRule;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.MerchantProviderRoute;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerMainWalletMapper;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.mapper.MerchantProviderRouteMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 路由读模型本地快照。
 * 决策阶段只读这里，禁止回源数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingSnapshotCacheService {

    private final ModelMapper modelMapper;
    private final PlanMapper planMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderTokenMapper providerTokenMapper;
    private final CustomerTokenMapper customerTokenMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerPlanMapper customerPlanMapper;
    private final CustomerMainWalletMapper customerMainWalletMapper;
    private final MerchantProviderRouteMapper merchantProviderRouteMapper;
    private final ObjectMapper objectMapper;

    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile Map<String, Model> modelByCodeCache = Collections.emptyMap();
    private volatile Map<Long, Provider> providerByIdCache = Collections.emptyMap();
    private volatile Map<Long, List<ProviderModel>> providerModelByModelIdCache = Collections.emptyMap();
    private volatile Map<Long, List<ProviderToken>> providerTokenByProviderIdCache = Collections.emptyMap();
    private volatile Map<Long, ProviderToken> providerTokenByIdCache = Collections.emptyMap();
    private volatile Map<String, CustomerToken> customerTokenByValueCache = Collections.emptyMap();
    private volatile Map<Long, CustomerToken> customerTokenByIdCache = Collections.emptyMap();
    private volatile Map<Long, CustomerAccount> customerAccountByIdCache = Collections.emptyMap();
    private volatile Map<Long, List<CustomerPlan>> customerPlanByAccountIdCache = Collections.emptyMap();
    private volatile Map<Long, CustomerMainWallet> customerMainWalletByAccountIdCache = Collections.emptyMap();
    private volatile Map<Long, AllowedModelsRule> customerAllowedModelsRuleByTokenIdCache = Collections.emptyMap();
    private volatile Map<Long, AllowedModelsRule> planAllowedModelsRuleByPlanIdCache = Collections.emptyMap();
    private volatile Map<Long, ProviderProtocolRule> providerProtocolRuleByProviderIdCache = Collections.emptyMap();
    private volatile Map<String, List<ProviderModel>> routingIndexByModelAndProtocolCache = Collections.emptyMap();
    private volatile Map<String, Long> merchantPreferredProviderCache = Collections.emptyMap();

    @PostConstruct
    public void init() {
        refreshAll();
    }

    @Scheduled(fixedDelayString = "${xlinks.router.cache.refresh-interval-ms:300000}")
    public void scheduledRefresh() {
        refreshAll();
    }

    public void refreshAll() {
        withRefreshLock(() -> {
            long startedAt = System.currentTimeMillis();
            List<Model> models = modelMapper.selectList(new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1));
            List<Provider> providers = providerMapper.selectList(new LambdaQueryWrapper<Provider>().eq(Provider::getStatus, 1));
            List<ProviderModel> providerModels = providerModelMapper.selectList(
                    new LambdaQueryWrapper<ProviderModel>().eq(ProviderModel::getStatus, 1).eq(ProviderModel::getDeleted, 0)
            );
            List<ProviderToken> providerTokens = providerTokenMapper.selectList(
                    new LambdaQueryWrapper<ProviderToken>().eq(ProviderToken::getTokenStatus, 1)
            );
            List<CustomerToken> customerTokens = customerTokenMapper.selectList(new LambdaQueryWrapper<>());
            List<CustomerAccount> customerAccounts = customerAccountMapper.selectList(
                    new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getStatus, 1).eq(CustomerAccount::getDeleted, 0)
            );
            List<CustomerPlan> customerPlans = customerPlanMapper.selectList(new LambdaQueryWrapper<>());
            List<CustomerMainWallet> wallets = customerMainWalletMapper.selectList(
                    new LambdaQueryWrapper<CustomerMainWallet>().eq(CustomerMainWallet::getDeleted, 0)
            );
            List<Plan> plans = planMapper.selectList(new LambdaQueryWrapper<Plan>().select(Plan::getId, Plan::getAllowedModels));
            List<MerchantProviderRoute> routes = merchantProviderRouteMapper.selectList(new LambdaQueryWrapper<>());

            applySnapshot(buildSnapshot(models, providers, providerModels, providerTokens, customerTokens,
                    customerAccounts, customerPlans, wallets, plans, routes));
            log.info("路由快照刷新完成。models={} providers={} providerModels={} providerTokens={} customerTokens={} customerPlans={} wallets={} routes={}",
                    modelByCodeCache.size(), providerByIdCache.size(), providerModelByModelIdCache.size(),
                    providerTokenByIdCache.size(), customerTokenByValueCache.size(), customerPlanByAccountIdCache.size(),
                    customerMainWalletByAccountIdCache.size(), merchantPreferredProviderCache.size());
            log.info("Routing snapshot refresh elapsedMs={}", Math.max(System.currentTimeMillis() - startedAt, 0L));
        });
    }

    public void refreshModelByCode(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return;
        }
        withRefreshLock(() -> {
            Model model = modelMapper.selectOne(new LambdaQueryWrapper<Model>()
                    .eq(Model::getModelCode, modelCode)
                    .eq(Model::getStatus, 1));
            if (model == null) {
                return;
            }
            Map<String, Model> copy = new HashMap<>(modelByCodeCache);
            copy.put(model.getModelCode(), model);
            modelByCodeCache = Collections.unmodifiableMap(copy);
        });
    }

    public void refreshProviderById(Long providerId) {
        if (providerId == null) {
            return;
        }
        withRefreshLock(() -> {
            Provider provider = providerMapper.selectById(providerId);
            if (provider == null) {
                return;
            }
            Map<Long, Provider> copy = new HashMap<>(providerByIdCache);
            copy.put(providerId, provider);
            providerByIdCache = Collections.unmodifiableMap(copy);
            rebuildProviderDerivedCaches();
        });
    }

    public void refreshProviderModelByModelId(Long modelId) {
        if (modelId == null) {
            return;
        }
        withRefreshLock(() -> {
            List<ProviderModel> providerModels = providerModelMapper.selectList(
                    new LambdaQueryWrapper<ProviderModel>()
                            .eq(ProviderModel::getModelId, modelId)
                            .eq(ProviderModel::getStatus, 1)
                            .eq(ProviderModel::getDeleted, 0)
            );
            Map<Long, List<ProviderModel>> copy = new HashMap<>(providerModelByModelIdCache);
            copy.put(modelId, List.copyOf(providerModels == null ? List.of() : providerModels));
            providerModelByModelIdCache = Collections.unmodifiableMap(copy);
            rebuildRoutingIndex();
        });
    }

    public void refreshProviderTokensByProviderId(Long providerId) {
        if (providerId == null) {
            return;
        }
        withRefreshLock(() -> {
            List<ProviderToken> providerTokens = providerTokenMapper.selectList(
                    new LambdaQueryWrapper<ProviderToken>()
                            .eq(ProviderToken::getProviderId, providerId)
                            .eq(ProviderToken::getTokenStatus, 1)
            );
            Map<Long, List<ProviderToken>> byProviderCopy = new HashMap<>(providerTokenByProviderIdCache);
            Map<Long, ProviderToken> byIdCopy = new HashMap<>(providerTokenByIdCache);
            List<ProviderToken> immutable = List.copyOf(providerTokens == null ? List.of() : providerTokens);
            byProviderCopy.put(providerId, immutable);
            providerTokenByProviderIdCache = Collections.unmodifiableMap(byProviderCopy);
            for (ProviderToken token : immutable) {
                if (token != null && token.getId() != null) {
                    byIdCopy.put(token.getId(), token);
                }
            }
            providerTokenByIdCache = Collections.unmodifiableMap(byIdCopy);
        });
    }

    public void refreshPlanById(Long planId) {
        if (planId == null) {
            return;
        }
        withRefreshLock(() -> {
            Plan plan = planMapper.selectById(planId);
            if (plan == null) {
                return;
            }
            Map<Long, AllowedModelsRule> copy = new HashMap<>(planAllowedModelsRuleByPlanIdCache);
            copy.put(planId, parseAllowedModelsRule(plan.getAllowedModels()));
            planAllowedModelsRuleByPlanIdCache = Collections.unmodifiableMap(copy);
        });
    }

    public void refreshCustomerTokenById(Long tokenId) {
        if (tokenId == null) {
            return;
        }
        withRefreshLock(() -> {
            CustomerToken token = customerTokenMapper.selectById(tokenId);
            if (token == null) {
                return;
            }
            cacheCustomerToken(token);
        });
    }

    public void refreshCustomerTokenByAccountId(Long accountId) {
        if (accountId == null) {
            return;
        }
        withRefreshLock(() -> {
            List<CustomerToken> tokens = customerTokenMapper.selectList(
                    new LambdaQueryWrapper<CustomerToken>().eq(CustomerToken::getAccountId, accountId)
            );
            Map<String, CustomerToken> byValueCopy = new HashMap<>(customerTokenByValueCache);
            Map<Long, CustomerToken> byIdCopy = new HashMap<>(customerTokenByIdCache);
            Map<Long, AllowedModelsRule> allowedModelCopy = new HashMap<>(customerAllowedModelsRuleByTokenIdCache);
            removeCustomerTokenEntries(byValueCopy, byIdCopy, allowedModelCopy, accountId);
            for (CustomerToken token : tokens == null ? List.<CustomerToken>of() : tokens) {
                if (token == null) {
                    continue;
                }
                putCustomerTokenEntries(byValueCopy, byIdCopy, allowedModelCopy, token);
            }
            customerTokenByValueCache = Collections.unmodifiableMap(byValueCopy);
            customerTokenByIdCache = Collections.unmodifiableMap(byIdCopy);
            customerAllowedModelsRuleByTokenIdCache = Collections.unmodifiableMap(allowedModelCopy);
        });
    }

    public void refreshWalletByAccountId(Long accountId) {
        if (accountId == null) {
            return;
        }
        withRefreshLock(() -> {
            CustomerMainWallet wallet = customerMainWalletMapper.selectOne(
                    new LambdaQueryWrapper<CustomerMainWallet>()
                            .eq(CustomerMainWallet::getAccountId, accountId)
                            .eq(CustomerMainWallet::getDeleted, 0)
                            .last("limit 1")
            );
            if (wallet == null) {
                Map<Long, CustomerMainWallet> copy = new HashMap<>(customerMainWalletByAccountIdCache);
                copy.remove(accountId);
                customerMainWalletByAccountIdCache = Collections.unmodifiableMap(copy);
                return;
            }
            Map<Long, CustomerMainWallet> copy = new HashMap<>(customerMainWalletByAccountIdCache);
            copy.put(accountId, wallet);
            customerMainWalletByAccountIdCache = Collections.unmodifiableMap(copy);
        });
    }

    public Model getModelByCode(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return null;
        }
        return modelByCodeCache.get(modelCode);
    }

    public List<Model> listModels() {
        if (modelByCodeCache.isEmpty()) {
            return List.of();
        }
        return modelByCodeCache.values().stream()
                .sorted(Comparator.comparing(model -> model.getModelCode() == null ? "" : model.getModelCode()))
                .toList();
    }

    public CustomerToken getCustomerTokenByValue(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return null;
        }
        return customerTokenByValueCache.get(tokenValue);
    }

    public CustomerToken getCustomerTokenById(Long tokenId) {
        if (tokenId == null) {
            return null;
        }
        return customerTokenByIdCache.get(tokenId);
    }

    public CustomerAccount getCustomerAccountById(Long accountId) {
        if (accountId == null) {
            return null;
        }
        return customerAccountByIdCache.get(accountId);
    }

    public CustomerMainWallet getCustomerMainWalletByAccountId(Long accountId) {
        if (accountId == null) {
            return null;
        }
        return customerMainWalletByAccountIdCache.get(accountId);
    }

    public List<CustomerPlan> getAvailableCustomerPlans(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        List<CustomerPlan> plans = customerPlanByAccountIdCache.get(accountId);
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        return plans.stream()
                .filter(plan -> isPlanUsable(plan, now, today))
                .toList();
    }

    public AllowedModelsRule getCustomerAllowedModelsRule(Long tokenId) {
        if (tokenId == null) {
            return AllowedModelsRule.allowAll();
        }
        return customerAllowedModelsRuleByTokenIdCache.getOrDefault(tokenId, AllowedModelsRule.allowAll());
    }

    public AllowedModelsRule getPlanAllowedModelsRule(Long planId) {
        if (planId == null) {
            return AllowedModelsRule.allowAll();
        }
        return planAllowedModelsRuleByPlanIdCache.getOrDefault(planId, AllowedModelsRule.allowAll());
    }

    public Provider getProvider(Long providerId) {
        if (providerId == null) {
            return null;
        }
        return providerByIdCache.get(providerId);
    }

    public List<ProviderToken> getProviderTokens(Long providerId) {
        if (providerId == null) {
            return List.of();
        }
        return providerTokenByProviderIdCache.getOrDefault(providerId, List.of());
    }

    public List<ProviderModel> getRoutingIndex(Long modelId, ForwardProtocol protocol) {
        if (modelId == null || protocol == null) {
            return List.of();
        }
        List<ProviderModel> cached = routingIndexByModelAndProtocolCache.get(buildRoutingIndexKey(modelId, protocol));
        if (cached != null) {
            return cached;
        }
        List<ProviderModel> providerModels = providerModelByModelIdCache.getOrDefault(modelId, List.of());
        if (providerModels.isEmpty()) {
            return List.of();
        }
        List<ProviderModel> matched = providerModels.stream()
                .filter(model -> model != null && model.getProviderId() != null)
                .filter(model -> supportsProtocol(model.getProviderId(), protocol))
                .sorted((left, right) -> {
                    int cmp = Integer.compare(
                            resolveProviderPriority(providerByIdCache.get(right.getProviderId())),
                            resolveProviderPriority(providerByIdCache.get(left.getProviderId()))
                    );
                    if (cmp != 0) {
                        return cmp;
                    }
                    Long leftId = left.getId() == null ? Long.MAX_VALUE : left.getId();
                    Long rightId = right.getId() == null ? Long.MAX_VALUE : right.getId();
                    return Long.compare(leftId, rightId);
                })
                .toList();
        Map<String, List<ProviderModel>> copy = new HashMap<>(routingIndexByModelAndProtocolCache);
        copy.put(buildRoutingIndexKey(modelId, protocol), List.copyOf(matched));
        routingIndexByModelAndProtocolCache = Collections.unmodifiableMap(copy);
        return matched;
    }

    public Long getMerchantPreferredProvider(Long accountId, Long modelId) {
        if (accountId == null || modelId == null) {
            return null;
        }
        return merchantPreferredProviderCache.get(buildMerchantRouteKey(accountId, modelId));
    }

    private CacheSnapshot buildSnapshot(List<Model> models,
                                        List<Provider> providers,
                                        List<ProviderModel> providerModels,
                                        List<ProviderToken> providerTokens,
                                        List<CustomerToken> customerTokens,
                                        List<CustomerAccount> customerAccounts,
                                        List<CustomerPlan> customerPlans,
                                        List<CustomerMainWallet> wallets,
                                        List<Plan> plans,
                                        List<MerchantProviderRoute> routes) {
        Map<String, Model> nextModelByCode = buildModelCache(models);
        Map<Long, Provider> nextProviderById = buildProviderCache(providers);
        Map<Long, List<ProviderModel>> nextProviderModelByModelId = buildProviderModelCache(providerModels);
        Map<Long, List<ProviderToken>> nextProviderTokenByProviderId = buildProviderTokenCache(providerTokens);
        Map<Long, ProviderToken> nextProviderTokenById = buildProviderTokenByIdCache(providerTokens);
        Map<String, CustomerToken> nextCustomerTokenByValue = buildCustomerTokenByValueCache(customerTokens);
        Map<Long, CustomerToken> nextCustomerTokenById = buildCustomerTokenByIdCache(customerTokens);
        Map<Long, CustomerAccount> nextCustomerAccountById = buildCustomerAccountCache(customerAccounts);
        Map<Long, List<CustomerPlan>> nextCustomerPlanByAccountId = buildCustomerPlanCache(customerPlans);
        Map<Long, CustomerMainWallet> nextWalletByAccountId = buildWalletCache(wallets);
        Map<Long, AllowedModelsRule> nextCustomerAllowedModelsRule = buildCustomerAllowedModelsRuleCache(customerTokens);
        Map<Long, AllowedModelsRule> nextPlanAllowedModelsRule = buildPlanAllowedModelsRuleCache(plans);
        Map<Long, ProviderProtocolRule> nextProviderProtocolRule = buildProviderProtocolRuleCache(providers);
        Map<String, Long> nextMerchantPreferredProvider = buildMerchantPreferredProviderCache(routes);
        Map<String, List<ProviderModel>> nextRoutingIndex = buildRoutingIndex(nextProviderModelByModelId, nextProviderById, nextProviderProtocolRule);
        return new CacheSnapshot(nextModelByCode, nextProviderById, nextProviderModelByModelId, nextProviderTokenByProviderId,
                nextProviderTokenById, nextCustomerTokenByValue, nextCustomerTokenById, nextCustomerAccountById,
                nextCustomerPlanByAccountId, nextWalletByAccountId, nextCustomerAllowedModelsRule, nextPlanAllowedModelsRule,
                nextProviderProtocolRule, nextRoutingIndex, nextMerchantPreferredProvider);
    }

    private void applySnapshot(CacheSnapshot snapshot) {
        modelByCodeCache = snapshot.modelByCodeCache();
        providerByIdCache = snapshot.providerByIdCache();
        providerModelByModelIdCache = snapshot.providerModelByModelIdCache();
        providerTokenByProviderIdCache = snapshot.providerTokenByProviderIdCache();
        providerTokenByIdCache = snapshot.providerTokenByIdCache();
        customerTokenByValueCache = snapshot.customerTokenByValueCache();
        customerTokenByIdCache = snapshot.customerTokenByIdCache();
        customerAccountByIdCache = snapshot.customerAccountByIdCache();
        customerPlanByAccountIdCache = snapshot.customerPlanByAccountIdCache();
        customerMainWalletByAccountIdCache = snapshot.customerMainWalletByAccountIdCache();
        customerAllowedModelsRuleByTokenIdCache = snapshot.customerAllowedModelsRuleByTokenIdCache();
        planAllowedModelsRuleByPlanIdCache = snapshot.planAllowedModelsRuleByPlanIdCache();
        providerProtocolRuleByProviderIdCache = snapshot.providerProtocolRuleByProviderIdCache();
        routingIndexByModelAndProtocolCache = snapshot.routingIndexByModelAndProtocolCache();
        merchantPreferredProviderCache = snapshot.merchantPreferredProviderCache();
    }

    private void rebuildProviderDerivedCaches() {
        Map<Long, ProviderProtocolRule> nextProviderProtocolRule = buildProviderProtocolRuleCache(new ArrayList<>(providerByIdCache.values()));
        providerProtocolRuleByProviderIdCache = Collections.unmodifiableMap(nextProviderProtocolRule);
        rebuildRoutingIndex();
    }

    private void rebuildRoutingIndex() {
        routingIndexByModelAndProtocolCache = Collections.unmodifiableMap(
                buildRoutingIndex(providerModelByModelIdCache, providerByIdCache, providerProtocolRuleByProviderIdCache)
        );
    }

    private void withRefreshLock(Runnable runnable) {
        refreshLock.lock();
        try {
            runnable.run();
        } finally {
            refreshLock.unlock();
        }
    }

    private Map<String, Model> buildModelCache(List<Model> models) {
        if (models == null || models.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Model> map = new HashMap<>();
        for (Model model : models) {
            if (model != null && model.getModelCode() != null && !model.getModelCode().isBlank()) {
                map.put(model.getModelCode(), model);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, Provider> buildProviderCache(List<Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Provider> map = new HashMap<>();
        for (Provider provider : providers) {
            if (provider != null && provider.getId() != null) {
                map.put(provider.getId(), provider);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, List<ProviderModel>> buildProviderModelCache(List<ProviderModel> providerModels) {
        if (providerModels == null || providerModels.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ProviderModel>> grouped = new HashMap<>();
        for (ProviderModel providerModel : providerModels) {
            if (providerModel == null || providerModel.getModelId() == null) {
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
            if (token == null || token.getProviderId() == null) {
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
            if (token != null && token.getId() != null) {
                map.put(token.getId(), token);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<String, CustomerToken> buildCustomerTokenByValueCache(List<CustomerToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, CustomerToken> map = new HashMap<>();
        for (CustomerToken token : tokens) {
            if (token != null && token.getTokenValue() != null && !token.getTokenValue().isBlank()) {
                map.put(token.getTokenValue(), token);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, CustomerToken> buildCustomerTokenByIdCache(List<CustomerToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CustomerToken> map = new HashMap<>();
        for (CustomerToken token : tokens) {
            if (token != null && token.getId() != null) {
                map.put(token.getId(), token);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, CustomerAccount> buildCustomerAccountCache(List<CustomerAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CustomerAccount> map = new HashMap<>();
        for (CustomerAccount account : accounts) {
            if (account != null && account.getId() != null) {
                map.put(account.getId(), account);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, List<CustomerPlan>> buildCustomerPlanCache(List<CustomerPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<CustomerPlan>> grouped = new HashMap<>();
        for (CustomerPlan plan : plans) {
            if (plan == null || plan.getAccountId() == null) {
                continue;
            }
            grouped.computeIfAbsent(plan.getAccountId(), ignored -> new ArrayList<>()).add(plan);
        }
        Map<Long, List<CustomerPlan>> immutable = new HashMap<>();
        for (Map.Entry<Long, List<CustomerPlan>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    private Map<Long, CustomerMainWallet> buildWalletCache(List<CustomerMainWallet> wallets) {
        if (wallets == null || wallets.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CustomerMainWallet> map = new HashMap<>();
        for (CustomerMainWallet wallet : wallets) {
            if (wallet != null && wallet.getAccountId() != null) {
                map.put(wallet.getAccountId(), wallet);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, AllowedModelsRule> buildCustomerAllowedModelsRuleCache(List<CustomerToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, AllowedModelsRule> map = new HashMap<>();
        for (CustomerToken token : tokens) {
            if (token != null && token.getId() != null) {
                map.put(token.getId(), parseAllowedModelsRule(token.getAllowedModels()));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, AllowedModelsRule> buildPlanAllowedModelsRuleCache(List<Plan> plans) {
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, AllowedModelsRule> map = new HashMap<>();
        for (Plan plan : plans) {
            if (plan != null && plan.getId() != null) {
                map.put(plan.getId(), parseAllowedModelsRule(plan.getAllowedModels()));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Long, ProviderProtocolRule> buildProviderProtocolRuleCache(List<Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ProviderProtocolRule> map = new HashMap<>();
        for (Provider provider : providers) {
            if (provider != null && provider.getId() != null) {
                map.put(provider.getId(), parseProviderProtocolRule(provider.getSupportedProtocols()));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<String, Long> buildMerchantPreferredProviderCache(List<MerchantProviderRoute> routes) {
        if (routes == null || routes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> map = new HashMap<>();
        for (MerchantProviderRoute route : routes) {
            if (route == null || route.getAccountId() == null || route.getModelId() == null || route.getProviderId() == null) {
                continue;
            }
            map.put(buildMerchantRouteKey(route.getAccountId(), route.getModelId()), route.getProviderId());
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<String, List<ProviderModel>> buildRoutingIndex(Map<Long, List<ProviderModel>> providerModelsByModelId,
                                                               Map<Long, Provider> providers,
                                                               Map<Long, ProviderProtocolRule> protocolRules) {
        if (providerModelsByModelId == null || providerModelsByModelId.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<ProviderModel>> index = new HashMap<>();
        for (Map.Entry<Long, List<ProviderModel>> entry : providerModelsByModelId.entrySet()) {
            Long modelId = entry.getKey();
            List<ProviderModel> mappings = entry.getValue();
            if (mappings == null || mappings.isEmpty()) {
                continue;
            }
            for (ForwardProtocol protocol : ForwardProtocol.values()) {
                List<ProviderModel> sorted = mappings.stream()
                        .filter(mapping -> mapping != null && mapping.getProviderId() != null)
                        .filter(mapping -> {
                            Provider provider = providers.get(mapping.getProviderId());
                            if (provider == null) {
                                return false;
                            }
                            ProviderProtocolRule rule = protocolRules.get(provider.getId());
                            return rule == null || rule.isAllowAll() || matchesProtocol(rule, protocol);
                        })
                        .sorted(Comparator
                                .comparingInt((ProviderModel mapping) -> resolveProviderPriority(providers.get(mapping.getProviderId())))
                                .reversed()
                                .thenComparing(mapping -> mapping.getId() == null ? Long.MAX_VALUE : mapping.getId()))
                        .toList();
                index.put(buildRoutingIndexKey(modelId, protocol), List.copyOf(sorted));
            }
        }
        return Collections.unmodifiableMap(index);
    }

    private boolean matchesProtocol(ProviderProtocolRule rule, ForwardProtocol protocol) {
        if (rule == null || protocol == null) {
            return false;
        }
        if (rule.isAllowAll()) {
            return true;
        }
        Set<String> normalizedProtocols = rule.getNormalizedProtocols();
        if (normalizedProtocols == null || normalizedProtocols.isEmpty()) {
            return true;
        }
        String code = normalize(protocol.getCode());
        return normalizedProtocols.contains(code);
    }

    private boolean supportsProtocol(Long providerId, ForwardProtocol protocol) {
        if (providerId == null || protocol == null) {
            return false;
        }
        ProviderProtocolRule rule = providerProtocolRuleByProviderIdCache.get(providerId);
        return rule == null || rule.isAllowAll() || matchesProtocol(rule, protocol);
    }

    private int resolveProviderPriority(Provider provider) {
        return provider == null || provider.getPriority() == null ? 0 : provider.getPriority();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String buildRoutingIndexKey(Long modelId, ForwardProtocol protocol) {
        return modelId + "#" + protocol.getCode();
    }

    private String buildMerchantRouteKey(Long accountId, Long modelId) {
        return accountId + "#" + modelId;
    }

    private AllowedModelsRule parseAllowedModelsRule(String rawAllowedModels) {
        if (rawAllowedModels == null || rawAllowedModels.isBlank()) {
            return AllowedModelsRule.allowAll();
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
                values = Arrays.stream(trimmed.split("[,;，]"))
                        .map(item -> item == null ? "" : item.trim())
                        .filter(item -> !item.isBlank())
                        .collect(Collectors.toSet());
            }
            if (values.isEmpty()) {
                return AllowedModelsRule.allowAll();
            }
            List<String> modelList = values.stream().sorted().toList();
            return new AllowedModelsRule(false, Collections.unmodifiableSet(values), modelList);
        } catch (Exception ex) {
            log.warn("解析允许模型列表失败。rawAllowedModels={}", rawAllowedModels, ex);
            return AllowedModelsRule.allowAll();
        }
    }

    private ProviderProtocolRule parseProviderProtocolRule(String rawSupportedProtocols) {
        if (rawSupportedProtocols == null || rawSupportedProtocols.isBlank()) {
            return new ProviderProtocolRule(true, Collections.emptySet());
        }
        Set<String> values = Arrays.stream(rawSupportedProtocols.split("[,;，]"))
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (values.isEmpty() || values.contains("*")) {
            return new ProviderProtocolRule(true, Collections.emptySet());
        }
        return new ProviderProtocolRule(false, Collections.unmodifiableSet(values));
    }

    private boolean isPlanUsable(CustomerPlan plan, LocalDateTime now, LocalDate today) {
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return false;
        }
        if (plan.getPlanExpireTime() != null && !plan.getPlanExpireTime().isAfter(now)) {
            return false;
        }
        BigDecimal totalQuota = defaultDecimal(plan.getTotalQuota());
        if (totalQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (defaultDecimal(plan.getTotalUsedQuota()).compareTo(totalQuota) >= 0) {
            return false;
        }
        BigDecimal dailyQuota = defaultDecimal(plan.getDailyQuota());
        if (dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal usedQuota = defaultDecimal(plan.getUsedQuota());
        LocalDateTime refreshTime = plan.getQuotaRefreshTime();
        boolean refreshedToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);
        return !refreshedToday || usedQuota.compareTo(dailyQuota) < 0;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void cacheCustomerToken(CustomerToken token) {
        if (token == null) {
            return;
        }
        Map<String, CustomerToken> byValueCopy = new HashMap<>(customerTokenByValueCache);
        Map<Long, CustomerToken> byIdCopy = new HashMap<>(customerTokenByIdCache);
        Map<Long, AllowedModelsRule> allowedModelCopy = new HashMap<>(customerAllowedModelsRuleByTokenIdCache);
        if (token.getTokenValue() != null && !token.getTokenValue().isBlank()) {
            byValueCopy.put(token.getTokenValue(), token);
        }
        if (token.getId() != null) {
            byIdCopy.put(token.getId(), token);
            allowedModelCopy.put(token.getId(), parseAllowedModelsRule(token.getAllowedModels()));
        }
        customerTokenByValueCache = Collections.unmodifiableMap(byValueCopy);
        customerTokenByIdCache = Collections.unmodifiableMap(byIdCopy);
        customerAllowedModelsRuleByTokenIdCache = Collections.unmodifiableMap(allowedModelCopy);
    }

    private void putCustomerTokenEntries(Map<String, CustomerToken> byValueCopy,
                                         Map<Long, CustomerToken> byIdCopy,
                                         Map<Long, AllowedModelsRule> allowedModelCopy,
                                         CustomerToken token) {
        if (token.getTokenValue() != null && !token.getTokenValue().isBlank()) {
            byValueCopy.put(token.getTokenValue(), token);
        }
        if (token.getId() != null) {
            byIdCopy.put(token.getId(), token);
            allowedModelCopy.put(token.getId(), parseAllowedModelsRule(token.getAllowedModels()));
        }
    }

    private void removeCustomerTokenEntries(Map<String, CustomerToken> byValueCopy,
                                            Map<Long, CustomerToken> byIdCopy,
                                            Map<Long, AllowedModelsRule> allowedModelCopy,
                                            Long accountId) {
        byValueCopy.entrySet().removeIf(entry -> entry.getValue() != null && Objects.equals(entry.getValue().getAccountId(), accountId));
        byIdCopy.entrySet().removeIf(entry -> entry.getValue() != null && Objects.equals(entry.getValue().getAccountId(), accountId));
        allowedModelCopy.keySet().removeIf(tokenId -> {
            CustomerToken token = customerTokenByIdCache.get(tokenId);
            return token != null && Objects.equals(token.getAccountId(), accountId);
        });
    }

    private record CacheSnapshot(Map<String, Model> modelByCodeCache,
                                 Map<Long, Provider> providerByIdCache,
                                 Map<Long, List<ProviderModel>> providerModelByModelIdCache,
                                 Map<Long, List<ProviderToken>> providerTokenByProviderIdCache,
                                 Map<Long, ProviderToken> providerTokenByIdCache,
                                 Map<String, CustomerToken> customerTokenByValueCache,
                                 Map<Long, CustomerToken> customerTokenByIdCache,
                                 Map<Long, CustomerAccount> customerAccountByIdCache,
                                 Map<Long, List<CustomerPlan>> customerPlanByAccountIdCache,
                                 Map<Long, CustomerMainWallet> customerMainWalletByAccountIdCache,
                                 Map<Long, AllowedModelsRule> customerAllowedModelsRuleByTokenIdCache,
                                 Map<Long, AllowedModelsRule> planAllowedModelsRuleByPlanIdCache,
                                 Map<Long, ProviderProtocolRule> providerProtocolRuleByProviderIdCache,
                                 Map<String, List<ProviderModel>> routingIndexByModelAndProtocolCache,
                                 Map<String, Long> merchantPreferredProviderCache) {
    }
}
