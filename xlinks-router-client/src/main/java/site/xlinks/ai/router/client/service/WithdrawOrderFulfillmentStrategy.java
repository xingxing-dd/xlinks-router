package site.xlinks.ai.router.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.entity.CustomerOrder;

/**
 * Fulfillment strategy for withdraw orders.
 */
@Component
@Slf4j
public class WithdrawOrderFulfillmentStrategy implements OrderFulfillmentStrategy {

    private static final String ORDER_TYPE = "withdraw";

    @Override
    public String orderType() {
        return ORDER_TYPE;
    }

    @Override
    public OrderFulfillmentResult fulfill(CustomerOrder order) {
        log.info("提现订单履约策略待接入资金账户模块, orderNo={}", order.getOrderNo());
        return OrderFulfillmentResult.skipped(ORDER_TYPE, "提现履约逻辑待实现");
    }
}

