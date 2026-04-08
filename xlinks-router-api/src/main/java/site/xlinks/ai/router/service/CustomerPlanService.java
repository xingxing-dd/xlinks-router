package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户套餐服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPlanService {

    private final CustomerPlanMapper customerPlanMapper;

    /**
     * 使用套餐额度（带悲观锁）
     */
    @Transactional
    public void consumeQuota(Long planRecordId, BigDecimal amount) {
        if (planRecordId == null || amount == null) {
            return;
        }
        CustomerPlan plan = customerPlanMapper.selectByIdForUpdate(planRecordId);
        if (plan == null) {
            return;
        }

        BigDecimal usedQuota = defaultDecimal(plan.getUsedQuota());
        BigDecimal dailyQuota = defaultDecimal(plan.getDailyQuota());
        BigDecimal usedIncrement = amount.max(BigDecimal.ZERO);
        BigDecimal updatedUsed = usedQuota.add(usedIncrement);

        plan.setUsedQuota(updatedUsed);
        plan.setTotalUsedQuota(defaultDecimal(plan.getTotalUsedQuota()).add(usedIncrement));

        if (updatedUsed.compareTo(dailyQuota) >= 0) {
            LocalDate today = LocalDate.now();
            LocalDateTime refreshTime = plan.getQuotaRefreshTime();
            boolean refreshToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);

            if (!refreshToday) {
                plan.setUsedQuota(updatedUsed.subtract(dailyQuota));
                plan.setQuotaRefreshTime(LocalDateTime.now());
            } else {
                plan.setUsedQuota(dailyQuota);
            }
        }

        customerPlanMapper.updateById(plan);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
