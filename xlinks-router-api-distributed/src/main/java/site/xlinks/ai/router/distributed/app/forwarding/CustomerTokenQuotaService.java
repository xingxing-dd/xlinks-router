package site.xlinks.ai.router.distributed.app.forwarding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.distributed.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.distributed.infrastructure.cache.RoutingSnapshotCacheService;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenQuotaService {

    private final CustomerTokenMapper customerTokenMapper;
    private final DistributedRouteCacheRepository distributedRouteCacheRepository;
    private final RoutingSnapshotCacheService routingSnapshotCacheService;

    public void syncQuotaUsage(Long tokenId, BigDecimal todayUsed, BigDecimal amount) {
        if (tokenId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        customerTokenMapper.syncQuotaUsage(
                tokenId,
                todayUsed == null ? BigDecimal.ZERO : todayUsed,
                amount
        );
        CustomerToken customerToken = customerTokenMapper.selectById(tokenId);
        if (customerToken != null) {
            distributedRouteCacheRepository.putCustomerTokenById(customerToken);
            if (customerToken.getTokenValue() != null && !customerToken.getTokenValue().isBlank()) {
                distributedRouteCacheRepository.putCustomerTokenByValue(customerToken);
            }
            routingSnapshotCacheService.refreshCustomerTokenById(customerToken.getId());
        }
    }

    @Scheduled(cron = "${xlinks.router.quota-reset.cron:0 0 0 * * ?}", zone = "${xlinks.router.quota-reset.zone:Asia/Shanghai}")
    public void resetDailyQuotaAtMidnight() {
        int affected = customerTokenMapper.resetDailyQuotaAtMidnight();
        routingSnapshotCacheService.refreshAll();
        log.info("客户令牌日额度重置完成。affected={}", affected);
    }
}
