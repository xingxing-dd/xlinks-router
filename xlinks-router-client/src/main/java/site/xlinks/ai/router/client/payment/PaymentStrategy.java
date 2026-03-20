package site.xlinks.ai.router.client.payment;

/**
 * 支付策略
 */
public interface PaymentStrategy {

    /**
     * 支付方式编码
     */
    String getMethod();

    /**
     * 发起支付
     */
    PaymentResult pay(PaymentRequest request);
}
