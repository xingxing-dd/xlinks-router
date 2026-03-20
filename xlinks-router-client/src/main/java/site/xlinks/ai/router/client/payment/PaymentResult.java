package site.xlinks.ai.router.client.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付结果
 */
@Getter
@AllArgsConstructor
public class PaymentResult {
    private final String orderId;
    private final String payUrl;
}
