package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.mapper.PlanMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fulfillment strategy for subscription purchase orders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPurchaseFulfillmentStrategy implements OrderFulfillmentStrategy {

    private static final String ORDER_TYPE = "subscription_purchase";
    private static final String PLAN_SOURCE_PREFIX = "order:";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CustomerPlanMapper customerPlanMapper;
    private final PlanMapper planMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String orderType() {
        return ORDER_TYPE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderFulfillmentResult fulfill(CustomerOrder order) {
        String source = PLAN_SOURCE_PREFIX + order.getOrderNo();
        CustomerPlan existing = customerPlanMapper.selectOne(new LambdaQueryWrapper<CustomerPlan>()
                .eq(CustomerPlan::getSource, source)
                .last("limit 1"));
        if (existing != null) {
            return OrderFulfillmentResult.fulfilled(ORDER_TYPE, existing.getId(), "已完成过履约");
        }

        Snapshot snapshot = parseSnapshot(order.getOrderInfo());
        Plan dbPlan = snapshot.planId == null ? null : planMapper.selectById(snapshot.planId);

        Long accountId = order.getAccountId();
        if (accountId == null) {
            log.warn("订单缺少 accountId，无法履约, orderNo={}", order.getOrderNo());
            return OrderFulfillmentResult.skipped(ORDER_TYPE, "订单缺少 accountId");
        }

        Long planId = firstNonNull(snapshot.planId, dbPlan == null ? null : dbPlan.getId(), null);
        if (planId == null) {
            log.warn("订单快照缺少 planId 且无法回退套餐数据，无法履约, orderNo={}", order.getOrderNo());
            return OrderFulfillmentResult.skipped(ORDER_TYPE, "缺少 planId");
        }

        Integer durationDays = firstNonNull(snapshot.durationDays, dbPlan == null ? null : dbPlan.getDurationDays(), 30);
        BigDecimal dailyQuota = firstNonNull(snapshot.dailyQuota, dbPlan == null ? null : dbPlan.getDailyQuota(), BigDecimal.ZERO);
        BigDecimal totalQuota = firstNonNull(snapshot.totalQuota, dbPlan == null ? null : dbPlan.getTotalQuota(), BigDecimal.ZERO);

        CustomerPlan customerPlan = new CustomerPlan();
        customerPlan.setAccountId(accountId);
        customerPlan.setPlanId(planId);
        customerPlan.setPlanName(firstNonNull(snapshot.planName, dbPlan == null ? null : dbPlan.getPlanName(), order.getOrderTitle()));
        customerPlan.setPrice(order.getTotalAmount());
        customerPlan.setDurationDays(durationDays);
        customerPlan.setDailyQuota(dailyQuota);
        customerPlan.setTotalQuota(totalQuota);
        customerPlan.setUsedQuota(BigDecimal.ZERO);
        customerPlan.setTotalUsedQuota(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        customerPlan.setQuotaRefreshTime(now);
        customerPlan.setPlanExpireTime(now.plusDays(durationDays));
        customerPlan.setStatus(1);
        customerPlan.setSource(source);
        customerPlan.setRemark("订单支付成功自动开通");
        customerPlan.setCreateBy(String.valueOf(accountId));
        customerPlan.setUpdateBy(String.valueOf(accountId));
        customerPlanMapper.insert(customerPlan);
        return OrderFulfillmentResult.fulfilled(ORDER_TYPE, customerPlan.getId(), "履约成功");
    }

    private Snapshot parseSnapshot(String orderInfo) {
        Snapshot snapshot = new Snapshot();
        if (orderInfo == null || orderInfo.isBlank()) {
            return snapshot;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(orderInfo, MAP_TYPE);
            snapshot.planId = toLong(map.get("planId"));
            snapshot.planName = toStringValue(map.get("planName"));
            snapshot.durationDays = toInteger(map.get("durationDays"));
            snapshot.dailyQuota = toBigDecimal(map.get("dailyQuota"));
            snapshot.totalQuota = toBigDecimal(map.get("totalQuota"));
        } catch (Exception ex) {
            log.warn("解析订单快照失败, orderInfo={}", orderInfo, ex);
        }
        return snapshot;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private <T> T firstNonNull(T first, T second, T fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback;
    }

    private static class Snapshot {
        private Long planId;
        private String planName;
        private Integer durationDays;
        private BigDecimal dailyQuota;
        private BigDecimal totalQuota;
    }
}

