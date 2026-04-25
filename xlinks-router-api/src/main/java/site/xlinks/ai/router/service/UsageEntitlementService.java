package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.model.wallet.WalletBundle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Usage entitlement decision service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEntitlementService {

    private final CustomerPlanMapper customerPlanMapper;
    private final RouteCacheService routeCacheService;
    private final WalletService walletService;

    /**
     * Decide available usage strategy for current request.
     */
    public UsageDecision decide(CustomerToken customerToken, String requestModel) {
        log.debug("Deciding usage type for customer: {}, model: {}",
                customerToken.getCustomerName(), requestModel);

        List<String> packageAllowedModels = routeCacheService.getCustomerTokenAllowedModels(customerToken);

        CustomerPlan plan = selectAvailablePlan(customerToken.getAccountId(), requestModel);
        boolean packageEnabled = plan != null;
        boolean balanceEnabled = !packageEnabled && hasUsableBalance(customerToken.getAccountId());

        int currentUsageType = calculateUsageType(packageEnabled, balanceEnabled,
                packageAllowedModels, requestModel);

        return UsageDecision.builder()
                .customerTokenId(customerToken.getId())
                .customerName(customerToken.getCustomerName())
                .planId(plan == null ? null : plan.getId())
                .packageEnabled(packageEnabled)
                .balanceEnabled(balanceEnabled)
                .currentUsageType(currentUsageType)
                .packageAllowedModels(packageAllowedModels)
                .unlimited(false)
                .multiplier(plan == null ? BigDecimal.ONE : defaultMultiplier(plan.getMultiplier()))
                .build();
    }

    /**
     * Select earliest-expiring available plan that supports requested model.
     */
    public CustomerPlan selectAvailablePlan(Long accountId, String requestModel) {
        if (accountId == null || !StringUtils.hasText(requestModel)) {
            return null;
        }

        List<CustomerPlan> availablePlans = customerPlanMapper.selectAvailablePlans(accountId, LocalDate.now());
        if (availablePlans == null || availablePlans.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        for (CustomerPlan availablePlan : availablePlans) {
            if (!isPlanCurrentlyUsable(availablePlan, now, today)) {
                continue;
            }
            if (routeCacheService.isModelSupportedByPlan(availablePlan.getPlanId(), requestModel)) {
                log.info("Selected available plan for account={}, model={}, planRecordId={}, planId={}",
                        accountId, requestModel, availablePlan.getId(), availablePlan.getPlanId());
                return availablePlan;
            }
        }

        log.info("No available plan supports model, account={}, model={}", accountId, requestModel);
        return null;
    }

    /**
     * Defensive runtime guard: avoid using expired or over-quota plans even if query results are stale.
     */
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
        BigDecimal totalUsed = plan.getTotalUsedQuota() == null ? BigDecimal.ZERO : plan.getTotalUsedQuota();
        if (totalUsed.compareTo(totalQuota) >= 0) {
            return false;
        }

        BigDecimal dailyQuota = plan.getDailyQuota();
        if (dailyQuota == null || dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal usedQuota = plan.getUsedQuota() == null ? BigDecimal.ZERO : plan.getUsedQuota();
        LocalDateTime refreshTime = plan.getQuotaRefreshTime();
        boolean refreshedToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);
        if (refreshedToday && usedQuota.compareTo(dailyQuota) >= 0) {
            return false;
        }

        return true;
    }

    private boolean hasUsableBalance(Long accountId) {
        if (accountId == null) {
            return false;
        }
        try {
            WalletBundle bundle = walletService.ensureWallet(accountId);
            CustomerMainWallet wallet = bundle == null ? null : bundle.getMainWallet();
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
            return availableBalance.compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception ex) {
            log.warn("Failed to check wallet balance for account={}", accountId, ex);
            return false;
        }
    }

    /**
     * Usage type decision:
     * 0: package priority, 1: package only, 2: balance only.
     */
    private int calculateUsageType(boolean packageEnabled, boolean balanceEnabled,
                                   List<String> packageAllowedModels, String requestModel) {
        if (packageEnabled && balanceEnabled) {
            if (packageAllowedModels.isEmpty() || packageAllowedModels.contains(requestModel)) {
                return 0;
            } else {
                return 2;
            }
        } else if (packageEnabled) {
            return 1;
        } else if (balanceEnabled) {
            return 2;
        } else {
            return 0;
        }
    }

    private BigDecimal defaultMultiplier(BigDecimal multiplier) {
        return multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : multiplier;
    }
}
