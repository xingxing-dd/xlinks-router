package site.xlinks.ai.router.client.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ThirdPartyPayLink;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.ThirdPartyPayLinkMapper;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Third-party payment strategy.
 */
@Component
@RequiredArgsConstructor
public class ThirdPartyPaymentStrategy implements PaymentStrategy {

    private static final String TARGET_TYPE_PLAN = "plan";
    private static final String ORDER_TYPE_SUBSCRIPTION_PURCHASE = "subscription_purchase";

    private final ThirdPartyPayLinkMapper thirdPartyPayLinkMapper;
    private final CustomerOrderMapper customerOrderMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getMethod() {
        return "third-party";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        if (request.getPlan() == null || request.getPlan().getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Plan information is required");
        }

        QueryWrapper<ThirdPartyPayLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("target_type", TARGET_TYPE_PLAN)
                .eq("target_id", request.getPlan().getId())
                .eq("status", 1)
                .last("limit 1");

        ThirdPartyPayLink payLink = thirdPartyPayLinkMapper.selectOne(queryWrapper);
        if (payLink == null || payLink.getPayUrl() == null || payLink.getPayUrl().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pay link does not exist");
        }

        ensureOrderNotExists(request.getOrderId());
        saveCustomerOrder(request, payLink);
        return new PaymentResult(request.getOrderId(), payLink.getPayUrl(), request.getExpiredAt());
    }

    private void ensureOrderNotExists(String orderNo) {
        Long count = customerOrderMapper.selectCount(
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(CustomerOrder::getOrderNo, orderNo)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Order already exists: " + orderNo);
        }
    }

    private void saveCustomerOrder(PaymentRequest request, ThirdPartyPayLink payLink) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(request.getOrderId());
        order.setAccountId(request.getAccountId());
        order.setOrderType(ORDER_TYPE_SUBSCRIPTION_PURCHASE);
        order.setOrderTitle(request.getPlan().getPlanName());
        order.setOrderInfo(buildOrderInfo(request, payLink));
        order.setPaymentChannel(request.getPaymentMethod());
        order.setTotalAmount(request.getAmount());
        order.setStatus(0);
        order.setExpiredAt(request.getExpiredAt());
        order.setRemark("Third-party order created");
        customerOrderMapper.insert(order);
    }

    private String buildOrderInfo(PaymentRequest request, ThirdPartyPayLink payLink) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("planId", request.getPlan().getId());
            snapshot.put("planName", request.getPlan().getPlanName());
            snapshot.put("durationDays", request.getPlan().getDurationDays());
            snapshot.put("dailyQuota", request.getPlan().getDailyQuota());
            snapshot.put("totalQuota", request.getPlan().getTotalQuota());
            snapshot.put("payLinkId", payLink.getId());
            snapshot.put("payUrl", payLink.getPayUrl());
            snapshot.put("paymentMethod", request.getPaymentMethod());
            snapshot.put("expiredAt", request.getExpiredAt() == null ? null : request.getExpiredAt().toString());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ignored) {
            return "{}";
        }
    }
}

