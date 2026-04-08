package site.xlinks.ai.router.client.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.config.AlipayConfig;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alipay payment strategy.
 */
@Component
@RequiredArgsConstructor
public class AlipayPaymentStrategy implements PaymentStrategy {

    private static final String ORDER_TYPE_SUBSCRIPTION_PURCHASE = "subscription_purchase";

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final CustomerOrderMapper customerOrderMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getMethod() {
        return "alipay";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        try {
            ensureOrderNotExists(request.getOrderId());
            saveCustomerOrder(request);

            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
            alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
            alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(request.getOrderId());
            model.setTotalAmount(request.getAmount().toString());
            model.setSubject(request.getPlan().getPlanName());
            model.setBody(request.getPlan().getPlanName());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            alipayRequest.setBizModel(model);

            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
            if (!response.isSuccess()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Alipay order creation failed: " + response.getSubMsg());
            }

            return new PaymentResult(request.getOrderId(), response.getBody(), request.getExpiredAt());
        } catch (AlipayApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Alipay order creation exception: " + e.getMessage());
        }
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

    private void saveCustomerOrder(PaymentRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(request.getOrderId());
        order.setAccountId(request.getAccountId());
        order.setOrderType(ORDER_TYPE_SUBSCRIPTION_PURCHASE);
        order.setOrderTitle(request.getPlan().getPlanName());
        order.setOrderInfo(buildOrderInfo(request));
        order.setPaymentChannel(request.getPaymentMethod());
        order.setTotalAmount(request.getAmount());
        order.setStatus(0);
        order.setExpiredAt(request.getExpiredAt());
        order.setRemark("Alipay order created");
        customerOrderMapper.insert(order);
    }

    private String buildOrderInfo(PaymentRequest request) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("planId", request.getPlan().getId());
            snapshot.put("planName", request.getPlan().getPlanName());
            snapshot.put("durationDays", request.getPlan().getDurationDays());
            snapshot.put("dailyQuota", request.getPlan().getDailyQuota());
            snapshot.put("totalQuota", request.getPlan().getTotalQuota());
            snapshot.put("paymentMethod", request.getPaymentMethod());
            snapshot.put("expiredAt", request.getExpiredAt() == null ? null : request.getExpiredAt().toString());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ignored) {
            return "{}";
        }
    }
}

