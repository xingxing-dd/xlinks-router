package site.xlinks.ai.router.client.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.config.AlipayConfig;
import site.xlinks.ai.router.client.dto.plan.CreateOrderResponse;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wallet recharge order service.
 */
@Service
@RequiredArgsConstructor
public class WalletOrderService {

    private static final int DEFAULT_EXPIRE_MINUTES = 30;
    private static final BigDecimal RECHARGE_PAYMENT_RATE = new BigDecimal("0.2");
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final CustomerOrderMapper customerOrderMapper;
    private final ObjectMapper objectMapper;

    public CreateOrderResponse createRechargeOrder(Long accountId, BigDecimal amount, String paymentMethod) {
        BigDecimal rechargeAmount = normalizeAmount(amount);
        if (!"alipay".equalsIgnoreCase(paymentMethod)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "wallet recharge currently only supports alipay");
        }
        BigDecimal payAmount = calculatePayAmount(rechargeAmount);

        String orderNo = generateOrderNo("RECH");
        ensureOrderNotExists(orderNo);
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(DEFAULT_EXPIRE_MINUTES);
        saveOrder(accountId, orderNo, rechargeAmount, payAmount, expiredAt, paymentMethod);

        try {
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
            alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
            alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(orderNo);
            model.setTotalAmount(payAmount.toPlainString());
            model.setSubject("Wallet Recharge");
            model.setBody("Wallet Recharge " + rechargeAmount.toPlainString() + " USD");
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            alipayRequest.setBizModel(model);

            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
            if (!response.isSuccess()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Alipay order creation failed: " + response.getSubMsg());
            }

            CreateOrderResponse result = new CreateOrderResponse();
            result.setOrderId(orderNo);
            result.setPayUrl(response.getBody());
            result.setExpireTime(expiredAt.format(DISPLAY_TIME_FORMATTER));
            return result;
        } catch (AlipayApiException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Alipay order creation exception: " + ex.getMessage());
        }
    }

    private void saveOrder(Long accountId,
                           String orderNo,
                           BigDecimal rechargeAmount,
                           BigDecimal payAmount,
                           LocalDateTime expiredAt,
                           String paymentMethod) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(orderNo);
        order.setAccountId(accountId);
        order.setOrderType(WalletConstants.ORDER_TYPE_RECHARGE);
        order.setOrderTitle("Wallet Recharge");
        order.setOrderInfo(buildRechargeOrderInfo(rechargeAmount, payAmount, paymentMethod));
        order.setPaymentChannel(paymentMethod);
        order.setTotalAmount(payAmount);
        order.setStatus(0);
        order.setExpiredAt(expiredAt);
        order.setRemark("Wallet recharge order created");
        customerOrderMapper.insert(order);
    }

    private void ensureOrderNotExists(String orderNo) {
        Long count = customerOrderMapper.selectCount(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "order already exists: " + orderNo);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "amount is required");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "amount must be greater than 0");
        }
        return normalized;
    }

    private BigDecimal calculatePayAmount(BigDecimal rechargeAmount) {
        return rechargeAmount.multiply(RECHARGE_PAYMENT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildRechargeOrderInfo(BigDecimal rechargeAmount, BigDecimal payAmount, String paymentMethod) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("scene", "recharge");
        snapshot.put("rechargeAmount", rechargeAmount.toPlainString());
        snapshot.put("payAmount", payAmount.toPlainString());
        snapshot.put("paymentMethod", paymentMethod);
        snapshot.put("paymentRate", RECHARGE_PAYMENT_RATE.toPlainString());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "failed to build recharge order info");
        }
    }

    private String generateOrderNo(String prefix) {
        return prefix + ORDER_TIME_FORMATTER.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
