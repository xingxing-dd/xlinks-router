package site.xlinks.ai.router.client.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import site.xlinks.ai.router.entity.Plan;

import java.math.BigDecimal;

/**
 * 支付请求参数
 */
@Getter
@AllArgsConstructor
public class PaymentRequest {
    private final String orderId;
    private final Plan plan;
    private final BigDecimal amount;
    private final String paymentMethod;
}
