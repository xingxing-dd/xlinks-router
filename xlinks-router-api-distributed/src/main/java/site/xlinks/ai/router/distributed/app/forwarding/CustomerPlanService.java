package site.xlinks.ai.router.distributed.app.forwarding;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.distributed.infrastructure.cache.RoutingSnapshotCacheService;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerPlanService {

    private final CustomerPlanMapper customerPlanMapper;
    private final RoutingSnapshotCacheService routingSnapshotCacheService;

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
        LocalDateTime now = LocalDateTime.now();

        plan.setUsedQuota(updatedUsed);
        plan.setTotalUsedQuota(defaultDecimal(plan.getTotalUsedQuota()).add(usedIncrement));

        if (dailyQuota.compareTo(BigDecimal.ZERO) > 0 && updatedUsed.compareTo(dailyQuota) >= 0) {
            LocalDate today = LocalDate.now();
            LocalDateTime refreshTime = plan.getQuotaRefreshTime();
            boolean refreshToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);

            if (!refreshToday) {
                BigDecimal refreshedUsed = updatedUsed.subtract(dailyQuota);
                if (refreshedUsed.compareTo(dailyQuota) > 0) {
                    refreshedUsed = dailyQuota;
                }
                if (refreshedUsed.compareTo(BigDecimal.ZERO) < 0) {
                    refreshedUsed = BigDecimal.ZERO;
                }
                plan.setUsedQuota(refreshedUsed);
                plan.setQuotaRefreshTime(now);
            } else {
                plan.setUsedQuota(dailyQuota);
            }
        }

        customerPlanMapper.updateById(plan);
        routingSnapshotCacheService.refreshAll();
    }

    @Scheduled(cron = "${xlinks.router.quota-reset.cron:0 0 0 * * ?}", zone = "${xlinks.router.quota-reset.zone:Asia/Shanghai}")
    @Transactional
    public void resetDailyQuotaAtMidnight() {
        customerPlanMapper.resetDailyQuotaAtMidnight(LocalDate.now().atStartOfDay());
        routingSnapshotCacheService.refreshAll();
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
