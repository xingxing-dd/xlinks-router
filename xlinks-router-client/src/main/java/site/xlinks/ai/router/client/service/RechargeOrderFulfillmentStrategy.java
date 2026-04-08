package site.xlinks.ai.router.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.entity.CustomerOrder;

/**
 * Fulfillment strategy for recharge orders.
 */
@Component
@Slf4j
public class RechargeOrderFulfillmentStrategy implements OrderFulfillmentStrategy {

    private static final String ORDER_TYPE = "recharge";

    @Override
    public String orderType() {
        return ORDER_TYPE;
    }

    @Override
    public OrderFulfillmentResult fulfill(CustomerOrder order) {
        log.info("充值订单履约策略待接入资金账户模块, orderNo={}", order.getOrderNo());
        return OrderFulfillmentResult.skipped(ORDER_TYPE, "充值履约逻辑待实现");
    }
}

