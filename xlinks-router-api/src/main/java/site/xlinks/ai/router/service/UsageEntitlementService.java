package site.xlinks.ai.router.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权益判定服务
 * 根据客户的套餐和余额状态，决定本次请求应该使用套餐还是余额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEntitlementService {

    private final ObjectMapper objectMapper;
    private final CustomerPlanMapper customerPlanMapper;

    /**
     * 判定当前请求的使用类型
     *
     * @param customerToken 客户 Token
     * @param requestModel 请求的模型名称
     * @return 权益判定结果
     */
    public UsageDecision decide(CustomerToken customerToken, String requestModel) {
        log.debug("Deciding usage type for customer: {}, model: {}",
                customerToken.getCustomerName(), requestModel);

        // 解析套餐允许的模型列表（来自客户 Token 的 allowedModels）
        List<String> packageAllowedModels = parseAllowedModels(customerToken.getAllowedModels());

        // 余额逻辑暂未实现，先预留
        boolean balanceEnabled = false;

        // 套餐逻辑：按过期时间升序，过滤可用套餐
        CustomerPlan plan = selectAvailablePlan(customerToken.getAccountId());
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
                .build();
    }

    public CustomerPlan selectAvailablePlan(Long accountId) {
        if (accountId == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        CustomerPlan available = customerPlanMapper.selectFirstAvailablePlan(accountId, today);
        log.info("获取到当前用户{}的可用套餐:{}", accountId, available);
//
//        // Fallback for old rows where used_quota may be null and cannot satisfy SQL predicate.
//        List<CustomerPlan> plans = customerPlanMapper.selectList(
//                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CustomerPlan>()
//                        .eq(CustomerPlan::getAccountId, accountId)
//                        .eq(CustomerPlan::getStatus, 1)
//                        .orderByAsc(CustomerPlan::getPlanExpireTime)
//        );
//        if (plans == null || plans.isEmpty()) {
//            return null;
//        }
//        for (CustomerPlan plan : plans) {
//            if (isPlanAvailable(plan, today)) {
//                return plan;
//            }
//        }
        return available;
    }

    private boolean isPlanAvailable(CustomerPlan plan, LocalDate today) {
        if (plan == null) {
            return false;
        }
        java.math.BigDecimal dailyQuota = plan.getDailyQuota();
        java.math.BigDecimal usedQuota = plan.getUsedQuota();
        if (dailyQuota == null || usedQuota == null) {
            return false;
        }
        if (dailyQuota.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (usedQuota.compareTo(dailyQuota) < 0) {
            return true;
        }
        LocalDateTime refreshTime = plan.getQuotaRefreshTime();
        if (refreshTime == null) {
            // 兼容历史脏数据：未记录刷新时间时，允许本次请求进入消费逻辑完成刷新判断。
            return true;
        }
        return !refreshTime.toLocalDate().isEqual(today);
    }

    /**
     * 计算使用类型
     *
     * 规则：
     * - 套餐和余额都启用(0): 优先套餐，如果请求模型不在套餐允许列表则切余额
     * - 仅套餐(1): 只能走套餐模式
     * - 仅余额(2): 只能走余额模式
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
     * 解析允许模型列表
     */
    private List<String> parseAllowedModels(String allowedModels) {
        if (allowedModels == null || allowedModels.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(allowedModels, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse allowed models: {}", allowedModels, e);
            return new ArrayList<>();
        }
    }
}
