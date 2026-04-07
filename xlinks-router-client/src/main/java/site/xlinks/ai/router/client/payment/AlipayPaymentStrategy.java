package site.xlinks.ai.router.client.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.config.AlipayConfig;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * 支付宝支付策略。
 */
@Component
@RequiredArgsConstructor
public class AlipayPaymentStrategy implements PaymentStrategy {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    @Override
    public String getMethod() {
        return "alipay";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        try {
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
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝下单失败: " + response.getSubMsg());
            }

            return new PaymentResult(request.getOrderId(), response.getBody());
        } catch (AlipayApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝下单异常: " + e.getMessage());
        }
    }
}