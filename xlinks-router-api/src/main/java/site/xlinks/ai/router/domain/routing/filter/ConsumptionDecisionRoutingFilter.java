package site.xlinks.ai.router.domain.routing.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.app.forwarding.model.ForwardingConsumptionMode;
import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消费方式决策过滤器。
 * 套餐和余额都只从本地快照判断，不在决策主链路查库。
 */
@Component
@Order(170)
@RequiredArgsConstructor
public class ConsumptionDecisionRoutingFilter implements RoutingFilter {

    private static final BigDecimal MINIMUM_USABLE_BALANCE = BigDecimal.ZERO;

    private final ForwardingReadModelLoader forwardingReadModelLoader;

    @Override
    public void filter(RoutingFilterContext context) {
        String modelCode = context.getRequest().getModel();
        CustomerPlan matchedPlan = selectAvailablePlan(context.getAccountId(), modelCode);

        if (matchedPlan != null) {
            context.setCustomerPlan(matchedPlan);
            context.setConsumptionMode(ForwardingConsumptionMode.PLAN);
        } else if (hasUsableBalance(context.getAccountId())) {
            context.setCustomerPlan(null);
            context.setConsumptionMode(ForwardingConsumptionMode.BALANCE);
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前客户没有可用的套餐或余额");
        }

        RequestChainLogCollector.bindCustomer(context.getCustomerToken(), context.getCustomerAccount(), context.getCustomerPlan());
        RequestChainLogCollector.record(
                RequestChainLogType.CONSUMPTION_DECIDED,
                context.getConsumptionMode().name(),
                context.getCustomerPlan() == null ? "-" : context.getCustomerPlan().getPlanName()
        );
    }

    private CustomerPlan selectAvailablePlan(Long accountId, String modelCode) {
        if (accountId == null || modelCode == null || modelCode.isBlank()) {
            return null;
        }
        List<CustomerPlan> availablePlans = forwardingReadModelLoader.loadAvailablePlans(accountId);
        if (availablePlans.isEmpty()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        for (CustomerPlan availablePlan : availablePlans) {
            if (!isPlanCurrentlyUsable(availablePlan, now, today)) {
                continue;
            }
            if (supportsModel(availablePlan, modelCode)) {
                return availablePlan;
            }
        }
        return null;
    }

    private boolean supportsModel(CustomerPlan customerPlan, String modelCode) {
        AllowedModelsRule rule = forwardingReadModelLoader.loadPlanAllowedModelsRule(customerPlan);
        if (rule == null || rule.isAllowAll()) {
            return true;
        }
        return rule.getAllowedModels() != null && rule.getAllowedModels().contains(modelCode.trim());
    }

    private boolean isPlanCurrentlyUsable(CustomerPlan plan, LocalDateTime now, LocalDate today) {
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return false;
        }
        if (plan.getPlanExpireTime() != null && !plan.getPlanExpireTime().isAfter(now)) {
            return false;
        }

        BigDecimal totalQuota = plan.getTotalQuota();
        if (totalQuota == null || totalQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal totalUsed = defaultDecimal(plan.getTotalUsedQuota());
        if (totalUsed.compareTo(totalQuota) >= 0) {
            return false;
        }

        BigDecimal dailyQuota = plan.getDailyQuota();
        if (dailyQuota == null || dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal usedQuota = defaultDecimal(plan.getUsedQuota());
        LocalDateTime refreshTime = plan.getQuotaRefreshTime();
        boolean refreshedToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);
        return !refreshedToday || usedQuota.compareTo(dailyQuota) < 0;
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
        return availableBalance.compareTo(MINIMUM_USABLE_BALANCE) > 0;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
