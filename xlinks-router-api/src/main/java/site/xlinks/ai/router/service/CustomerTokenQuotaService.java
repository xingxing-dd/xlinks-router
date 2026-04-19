package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;

import java.math.BigDecimal;

/**
 * Maintains customer token quota usage snapshots for fast quota checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenQuotaService {

    private final CustomerTokenMapper customerTokenMapper;
    private final RouteCacheService routeCacheService;

    public void syncQuotaUsage(Long tokenId, BigDecimal todayUsed, BigDecimal amount) {
        if (tokenId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        customerTokenMapper.syncQuotaUsage(
                tokenId,
                todayUsed == null ? BigDecimal.ZERO : todayUsed,
                amount
        );
        BigDecimal usedQuota = todayUsed == null ? BigDecimal.ZERO : todayUsed;
        BigDecimal totalUsedQuota = null;
        var customerToken = customerTokenMapper.selectById(tokenId);
        if (customerToken != null) {
            usedQuota = customerToken.getUsedQuota();
            totalUsedQuota = customerToken.getTotalUsedQuota();
        }
        routeCacheService.updateCustomerTokenQuota(tokenId, usedQuota, totalUsedQuota);
    }

    @Scheduled(cron = "${xlinks.router.quota-reset.cron:0 0 0 * * ?}", zone = "${xlinks.router.quota-reset.zone:Asia/Shanghai}")
    public void resetDailyQuotaAtMidnight() {
        int affected = customerTokenMapper.resetDailyQuotaAtMidnight();
        routeCacheService.refreshCustomerTokens();
        log.info("Customer token daily quota reset completed, affected tokens: {}", affected);
    }
}
