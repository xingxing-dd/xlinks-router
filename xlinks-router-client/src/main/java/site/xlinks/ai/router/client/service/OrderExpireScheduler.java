package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.time.LocalDateTime;

/**
 * Auto-close unpaid orders after expiration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpireScheduler {

    private final CustomerOrderMapper customerOrderMapper;

    @Scheduled(fixedDelayString = "${xlinks.client.order-expire-check-interval-ms:60000}")
    public void closeExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        int affected = customerOrderMapper.update(null, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getStatus, 0)
                .isNotNull(CustomerOrder::getExpiredAt)
                .le(CustomerOrder::getExpiredAt, now)
                .set(CustomerOrder::getStatus, 3)
                .set(CustomerOrder::getRemark, "订单已过期自动关闭")
                .set(CustomerOrder::getUpdatedAt, now));
        if (affected > 0) {
            log.info("自动关闭过期未支付订单数量: {}", affected);
        }
    }
}


