package site.xlinks.ai.router.distributed.app.forwarding;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderProtocolRule;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.MerchantProviderRoute;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.mapper.MerchantProviderRouteMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ForwardingReadModelLoader {

    private final DistributedRouteCacheRepository distributedRouteCacheRepository;
    private final ObjectMapper objectMapper;
    private final CustomerTokenMapper customerTokenMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerPlanMapper customerPlanMapper;
    private final ModelMapper modelMapper;
    private final PlanMapper planMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderTokenMapper providerTokenMapper;
    private final MerchantProviderRouteMapper merchantProviderRouteMapper;

    public CustomerToken loadCustomerToken(String tokenValue) {
        CustomerToken cached = distributedRouteCacheRepository.getCustomerTokenByValue(tokenValue);
        if (cached != null) {
            return cached;
        }

        CustomerToken customerToken = customerTokenMapper.selectOne(
                new LambdaQueryWrapper<CustomerToken>().eq(CustomerToken::getTokenValue, tokenValue)
        );
        if (customerToken == null || customerToken.getStatus() == null || customerToken.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CUSTOMER_TOKEN_INVALID, "Customer token invalid");
        }

        distributedRouteCacheRepository.putCustomerTokenByValue(customerToken);
        distributedRouteCacheRepository.putCustomerTokenById(customerToken);
        return customerToken;
    }

    public CustomerAccount loadCustomerAccount(Long accountId) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "Customer account not found");
        }
        CustomerAccount customerAccount = customerAccountMapper.selectById(accountId);
        if (customerAccount == null || customerAccount.getDeleted() != null && customerAccount.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "Customer account not found");
        }
        if (customerAccount.getStatus() == null || customerAccount.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Customer account disabled");
        }
        return customerAccount;
    }

    public CustomerPlan loadActivePlan(Long accountId) {
        if (accountId == null) {
            return null;
        }
        return customerPlanMapper.selectFirstAvailablePlan(accountId, LocalDate.now());
    }

    public Model loadModel(String modelCode) {
        Model cached = distributedRouteCacheRepository.getModelByCode(modelCode);
        if (cached != null) {
            return cached;
        }

        Model model = modelMapper.selectOne(
                new LambdaQueryWrapper<Model>()
                        .eq(Model::getModelCode, modelCode)
                        .eq(Model::getStatus, 1)
        );
        if (model == null) {
            throw new BusinessException(ErrorCode.MODEL_UNAVAILABLE, "Model unavailable");
        }
        distributedRouteCacheRepository.putModelByCode(model);
        return model;
    }

    public AllowedModelsRule loadCustomerAllowedModelsRule(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getId() == null) {
            return allowAllRule();
        }
        AllowedModelsRule cached = distributedRouteCacheRepository.getCustomerAllowedModelsRule(customerToken.getId());
        if (cached != null) {
            return cached;
        }
        AllowedModelsRule rule = parseAllowedModelsRule(customerToken.getAllowedModels());
        distributedRouteCacheRepository.putCustomerAllowedModelsRule(customerToken.getId(), rule);
        return rule;
    }

    public AllowedModelsRule loadPlanAllowedModelsRule(CustomerPlan customerPlan) {
        if (customerPlan == null || customerPlan.getPlanId() == null) {
            return allowAllRule();
        }
        AllowedModelsRule cached = distributedRouteCacheRepository.getPlanAllowedModelsRule(customerPlan.getPlanId());
        if (cached != null) {
            return cached;
        }
        Plan plan = planMapper.selectById(customerPlan.getPlanId());
        if (plan == null) {
            return allowAllRule();
        }
        AllowedModelsRule rule = parseAllowedModelsRule(plan.getAllowedModels());
        distributedRouteCacheRepository.putPlanAllowedModelsRule(plan.getId(), rule);
        return rule;
    }

    public List<ProviderModel> loadRoutingIndex(Long modelId, ForwardProtocol protocol) {
        List<ProviderModel> cached = distributedRouteCacheRepository.getRoutingIndex(modelId, protocol);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<ProviderModel> providerModels = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModel>()
                        .eq(ProviderModel::getModelId, modelId)
                        .eq(ProviderModel::getStatus, 1)
                        .eq(ProviderModel::getDeleted, 0)
        );
        if (providerModels.isEmpty()) {
            return List.of();
        }

        List<ProviderModel> matched = providerModels.stream()
                .filter(providerModel -> supportsProtocol(providerModel.getProviderId(), protocol))
                .sorted((left, right) -> {
                    int cmp = Integer.compare(resolveProviderPriority(right.getProviderId()), resolveProviderPriority(left.getProviderId()));
                    if (cmp != 0) {
                        return cmp;
                    }
                    Long leftId = left.getId() == null ? Long.MAX_VALUE : left.getId();
                    Long rightId = right.getId() == null ? Long.MAX_VALUE : right.getId();
                    return Long.compare(leftId, rightId);
                })
                .toList();
        distributedRouteCacheRepository.putProviderModelsByModelId(modelId, providerModels);
        distributedRouteCacheRepository.putRoutingIndex(modelId, protocol, matched);
        return matched;
    }

    public Provider loadProvider(Long providerId) {
        Provider cached = distributedRouteCacheRepository.getProviderById(providerId);
        if (cached != null) {
            return cached;
        }
        Provider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            return null;
        }
        distributedRouteCacheRepository.putProviderById(provider);
        distributedRouteCacheRepository.putProviderProtocolRule(providerId, parseProviderProtocolRule(provider.getSupportedProtocols()));
        return provider;
    }

    public List<ProviderToken> loadProviderTokens(Long providerId) {
        List<ProviderToken> cached = distributedRouteCacheRepository.getProviderTokensByProviderId(providerId);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ProviderToken> providerTokens = providerTokenMapper.selectList(
                new LambdaQueryWrapper<ProviderToken>().eq(ProviderToken::getProviderId, providerId)
        );
        distributedRouteCacheRepository.putProviderTokensByProviderId(providerId, providerTokens);
        for (ProviderToken providerToken : providerTokens) {
            distributedRouteCacheRepository.putProviderTokenById(providerToken);
        }
        return providerTokens;
    }

    public Long loadMerchantPreferredProvider(Long accountId, Long modelId) {
        Long cached = distributedRouteCacheRepository.getMerchantPreferredProvider(accountId, modelId);
        if (cached != null) {
            return cached;
        }
        MerchantProviderRoute route = merchantProviderRouteMapper.selectOne(
                new LambdaQueryWrapper<MerchantProviderRoute>()
                        .eq(MerchantProviderRoute::getAccountId, accountId)
                        .eq(MerchantProviderRoute::getModelId, modelId)
                        .last("limit 1")
        );
        if (route == null || route.getProviderId() == null) {
            return null;
        }
        distributedRouteCacheRepository.putMerchantPreferredProvider(accountId, modelId, route.getProviderId());
        return route.getProviderId();
    }

    private AllowedModelsRule parseAllowedModelsRule(String rawAllowedModels) {
        if (rawAllowedModels == null || rawAllowedModels.isBlank()) {
            return allowAllRule();
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
                return allowAllRule();
            }
            List<String> modelList = values.stream().sorted().toList();
            return new AllowedModelsRule(false, Collections.unmodifiableSet(values), modelList);
        } catch (Exception ex) {
            return allowAllRule();
        }
    }

    private AllowedModelsRule allowAllRule() {
        return new AllowedModelsRule(true, Collections.emptySet(), Collections.emptyList());
    }

    private ProviderProtocolRule parseProviderProtocolRule(String rawSupportedProtocols) {
        if (rawSupportedProtocols == null || rawSupportedProtocols.isBlank()) {
            return new ProviderProtocolRule(true, Collections.emptySet());
        }
        Set<String> values = Arrays.stream(rawSupportedProtocols.split("[,;\uFF0C]"))
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (values.isEmpty() || values.contains("*")) {
            return new ProviderProtocolRule(true, Collections.emptySet());
        }
        return new ProviderProtocolRule(false, Collections.unmodifiableSet(values));
    }

    private boolean supportsProtocol(Long providerId, ForwardProtocol protocol) {
        if (providerId == null || protocol == null) {
            return false;
        }
        ProviderProtocolRule rule = distributedRouteCacheRepository.getProviderProtocolRule(providerId);
        if (rule == null) {
            Provider provider = loadProvider(providerId);
            if (provider == null) {
                return false;
            }
            rule = distributedRouteCacheRepository.getProviderProtocolRule(providerId);
        }
        return rule == null || rule.isAllowAll()
                || (rule.getNormalizedProtocols() != null
                && rule.getNormalizedProtocols().contains(protocol.getCode().toLowerCase(Locale.ROOT)));
    }

    private int resolveProviderPriority(Long providerId) {
        Provider provider = loadProvider(providerId);
        return provider == null || provider.getPriority() == null ? 0 : provider.getPriority();
    }
}
