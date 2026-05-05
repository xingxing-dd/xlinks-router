package site.xlinks.ai.router.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiModelsService {

    private static final BigDecimal BALANCE_OVERDRAFT_TOLERANCE = new BigDecimal("-0.01");

    private final ForwardingReadModelLoader forwardingReadModelLoader;
    private final ProtocolRequestContextResolver protocolRequestContextResolver;

    public Map<String, Object> listModels(HttpServletRequest request) {
        CustomerTokenResolver.ResolvedCustomerToken resolvedCustomerToken =
                protocolRequestContextResolver.resolveCustomerToken(request);

        CustomerToken cachedToken = forwardingReadModelLoader.loadCustomerToken(resolvedCustomerToken.value());
        CustomerToken customerToken = forwardingReadModelLoader.loadFreshCustomerToken(cachedToken, resolvedCustomerToken.value());
        validateCustomerToken(customerToken);
        CustomerAccount customerAccount = forwardingReadModelLoader.loadCustomerAccount(customerToken.getAccountId());
        RequestChainLogCollector.bindCustomer(customerToken, customerAccount, null);

        AllowedModelsRule customerRule = forwardingReadModelLoader.loadCustomerAllowedModelsRule(customerToken);
        List<CustomerPlan> availablePlans = forwardingReadModelLoader.loadAvailablePlans(customerToken.getAccountId());
        boolean hasUsableBalance = hasUsableBalance(customerToken.getAccountId());

        List<Map<String, Object>> models = forwardingReadModelLoader.loadEnabledModels().stream()
                .filter(model -> isModelVisible(model, customerRule, availablePlans, hasUsableBalance))
                .map(this::toOpenAiModel)
                .toList();

        RequestChainLogCollector.bindQueryResultCount(models.size());
        RequestChainLogCollector.markSuccess("模型列表查询成功");

        return Map.of(
                "object", "list",
                "data", models
        );
    }

    private void validateCustomerToken(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getStatus() == null || customerToken.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Customer token disabled");
        }
        if (customerToken.getExpireTime() != null && LocalDateTime.now().isAfter(customerToken.getExpireTime())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Customer token expired");
        }
    }

    private boolean isModelVisible(Model model,
                                   AllowedModelsRule customerRule,
                                   List<CustomerPlan> availablePlans,
                                   boolean hasUsableBalance) {
        if (model == null || model.getModelCode() == null || model.getModelCode().isBlank()) {
            return false;
        }
        String modelCode = model.getModelCode().trim();
        if (isDisallowed(customerRule, modelCode)) {
            return false;
        }
        if (hasUsableBalance) {
            return true;
        }
        return isAllowedByAnyPlan(availablePlans, modelCode);
    }

    private boolean isAllowedByAnyPlan(List<CustomerPlan> availablePlans, String modelCode) {
        if (availablePlans == null || availablePlans.isEmpty()) {
            return false;
        }
        for (CustomerPlan availablePlan : availablePlans) {
            AllowedModelsRule planRule = forwardingReadModelLoader.loadPlanAllowedModelsRule(availablePlan);
            if (!isDisallowed(planRule, modelCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUsableBalance(Long accountId) {
        if (accountId == null) {
            return false;
        }
        CustomerMainWallet wallet = forwardingReadModelLoader.loadCustomerMainWallet(accountId);
        if (wallet == null) {
            return false;
        }
        if (wallet.getStatus() == null || wallet.getStatus() != 1) {
            return false;
        }
        if (wallet.getAllowOut() == null || wallet.getAllowOut() != 1) {
            return false;
        }
        BigDecimal availableBalance = wallet.getAvailableBalance() == null ? BigDecimal.ZERO : wallet.getAvailableBalance();
        return availableBalance.compareTo(BALANCE_OVERDRAFT_TOLERANCE) >= 0;
    }

    private boolean isDisallowed(AllowedModelsRule rule, String modelCode) {
        if (rule == null || rule.isAllowAll()) {
            return false;
        }
        return rule.getAllowedModels() != null
                && !rule.getAllowedModels().isEmpty()
                && !rule.getAllowedModels().contains(modelCode);
    }

    private Map<String, Object> toOpenAiModel(Model model) {
        long created = model.getCreatedAt() == null ? 0L : model.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
        return Map.of(
                "id", model.getModelCode(),
                "object", "model",
                "created", created,
                "owned_by", "xlinks-router"
        );
    }
}
