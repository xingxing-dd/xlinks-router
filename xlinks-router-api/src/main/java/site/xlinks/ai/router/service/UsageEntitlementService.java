package site.xlinks.ai.router.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Usage entitlement decision service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEntitlementService {

    private final ObjectMapper objectMapper;
    private final CustomerPlanMapper customerPlanMapper;
    private final RouteCacheService routeCacheService;

    /**
     * Decide available usage strategy for current request.
     */
    public UsageDecision decide(CustomerToken customerToken, String requestModel) {
        log.debug("Deciding usage type for customer: {}, model: {}",
                customerToken.getCustomerName(), requestModel);

        List<String> packageAllowedModels = parseAllowedModels(customerToken.getAllowedModels());

        // Balance mode is reserved and currently disabled.
        boolean balanceEnabled = false;

        CustomerPlan plan = selectAvailablePlan(customerToken.getAccountId(), requestModel);
        boolean packageEnabled = plan != null;

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

    /**
     * Parse allowed model list from JSON array or comma-separated string.
     */
    private List<String> parseAllowedModels(String allowedModels) {
        if (!StringUtils.hasText(allowedModels)) {
            return new ArrayList<>();
        }

        String trimmed = allowedModels.trim();
        try {
            if (trimmed.startsWith("[")) {
                List<String> models = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {
                });
                if (models == null) {
                    return Collections.emptyList();
                }
                return models.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .toList();
            }

            return java.util.Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse allowed models: {}", allowedModels, e);
            return new ArrayList<>();
        }
    }

    private BigDecimal defaultMultiplier(BigDecimal multiplier) {
        return multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : multiplier;
    }
}
