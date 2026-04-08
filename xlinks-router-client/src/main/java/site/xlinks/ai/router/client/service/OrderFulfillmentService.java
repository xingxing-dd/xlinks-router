package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order fulfillment dispatcher.
 */
@Service
@Slf4j
public class OrderFulfillmentService {

    private final CustomerOrderMapper customerOrderMapper;
    private final Map<String, OrderFulfillmentStrategy> strategyMap;

    public OrderFulfillmentService(CustomerOrderMapper customerOrderMapper,
                                   List<OrderFulfillmentStrategy> strategies) {
        this.customerOrderMapper = customerOrderMapper;
        this.strategyMap = new HashMap<>();
        for (OrderFulfillmentStrategy strategy : strategies) {
            String orderType = strategy.orderType();
            if (strategyMap.containsKey(orderType)) {
                throw new IllegalStateException("Duplicate order fulfillment strategy: " + orderType);
            }
            strategyMap.put(orderType, strategy);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderFulfillmentResult handlePaidOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return OrderFulfillmentResult.skipped(null, "订单号为空");
        }

        CustomerOrder order = customerOrderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            return OrderFulfillmentResult.skipped(null, "订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            return OrderFulfillmentResult.skipped(order.getOrderType(), "订单未支付成功");
        }

        OrderFulfillmentStrategy strategy = strategyMap.get(order.getOrderType());
        if (strategy == null) {
            log.warn("未找到订单履约策略, orderNo={}, orderType={}", orderNo, order.getOrderType());
            return OrderFulfillmentResult.skipped(order.getOrderType(), "未配置对应履约策略");
        }
        return strategy.fulfill(order);
    }
}

