package site.xlinks.ai.router.distributed.app.forwarding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.infrastructure.cache.RoutingSnapshotCacheService;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.util.List;

/**
 * 转发决策阶段只读门面。
 * 这里统一从本地快照取数，禁止在请求主链路中直接查库。
 */
@Component
@RequiredArgsConstructor
public class ForwardingReadModelLoader {

    private final RoutingSnapshotCacheService routingSnapshotCacheService;

    public CustomerToken loadCustomerToken(String tokenValue) {
        CustomerToken customerToken = routingSnapshotCacheService.getCustomerTokenByValue(tokenValue);
        if (customerToken == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_TOKEN_INVALID, "客户令牌不存在或已失效");
        }
        return customerToken;
    }

    /**
     * 保留该方法用于兼容现有调用方语义。
     * 当前实现不再回源数据库，只重新读取本地快照。
     */
    public CustomerToken loadFreshCustomerToken(CustomerToken cachedToken, String tokenValue) {
        CustomerToken latest = routingSnapshotCacheService.getCustomerTokenByValue(tokenValue);
        if (latest == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_TOKEN_INVALID, "客户令牌不存在或已失效");
        }
        if (cachedToken != null && cachedToken.getId() != null && latest.getId() != null
                && !cachedToken.getId().equals(latest.getId())) {
            throw new BusinessException(ErrorCode.CUSTOMER_TOKEN_INVALID, "客户令牌校验失败");
        }
        return latest;
    }

    public CustomerAccount loadCustomerAccount(Long accountId) {
        CustomerAccount customerAccount = routingSnapshotCacheService.getCustomerAccountById(accountId);
        if (customerAccount == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "客户账号不存在");
        }
        if (customerAccount.getDeleted() != null && customerAccount.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "客户账号不存在");
        }
        if (customerAccount.getStatus() == null || customerAccount.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "客户账号已禁用");
        }
        return customerAccount;
    }

    public CustomerPlan loadActivePlan(Long accountId) {
        List<CustomerPlan> plans = loadAvailablePlans(accountId);
        return plans.isEmpty() ? null : plans.get(0);
    }

    public List<CustomerPlan> loadAvailablePlans(Long accountId) {
        return routingSnapshotCacheService.getAvailableCustomerPlans(accountId);
    }

    public CustomerMainWallet loadCustomerMainWallet(Long accountId) {
        return routingSnapshotCacheService.getCustomerMainWalletByAccountId(accountId);
    }

    public Model loadModel(String modelCode) {
        Model model = routingSnapshotCacheService.getModelByCode(modelCode);
        if (model == null) {
            throw new BusinessException(ErrorCode.MODEL_UNAVAILABLE, "模型不可用");
        }
        return model;
    }

    public List<Model> loadEnabledModels() {
        return routingSnapshotCacheService.listModels();
    }

    public AllowedModelsRule loadCustomerAllowedModelsRule(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getId() == null) {
            return AllowedModelsRule.allowAll();
        }
        return routingSnapshotCacheService.getCustomerAllowedModelsRule(customerToken.getId());
    }

    public AllowedModelsRule loadPlanAllowedModelsRule(CustomerPlan customerPlan) {
        if (customerPlan == null || customerPlan.getPlanId() == null) {
            return AllowedModelsRule.allowAll();
        }
        return routingSnapshotCacheService.getPlanAllowedModelsRule(customerPlan.getPlanId());
    }

    public List<ProviderModel> loadRoutingIndex(Long modelId, ForwardProtocol protocol) {
        return routingSnapshotCacheService.getRoutingIndex(modelId, protocol);
    }

    public Provider loadProvider(Long providerId) {
        return routingSnapshotCacheService.getProvider(providerId);
    }

    public List<ProviderToken> loadProviderTokens(Long providerId) {
        return routingSnapshotCacheService.getProviderTokens(providerId);
    }

    public Long loadMerchantPreferredProvider(Long accountId, Long modelId) {
        return routingSnapshotCacheService.getMerchantPreferredProvider(accountId, modelId);
    }
}
