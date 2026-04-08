package site.xlinks.ai.router.client.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import site.xlinks.ai.router.entity.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付请求参数
 */
@Getter
@AllArgsConstructor
public class PaymentRequest {
    private final String orderId;
    private final Long accountId;
    private final Plan plan;
    private final BigDecimal amount;
    private final String paymentMethod;
    private final LocalDateTime expiredAt;
}
