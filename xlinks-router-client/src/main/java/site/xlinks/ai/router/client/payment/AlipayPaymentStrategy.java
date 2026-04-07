package site.xlinks.ai.router.client.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.config.AlipayConfig;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ThirdPartyPayOrder;
import site.xlinks.ai.router.mapper.ThirdPartyPayOrderMapper;

import java.time.LocalDateTime;

/**
 * 支付宝支付策略。
 */
@Component
@RequiredArgsConstructor
public class AlipayPaymentStrategy implements PaymentStrategy {

    private static final String TARGET_TYPE_PLAN = "plan";

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final ThirdPartyPayOrderMapper thirdPartyPayOrderMapper;

    @Override
    public String getMethod() {
        return "alipay";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        try {
            ensureOrderNotExists(request.getOrderId());
            saveThirdPartyPayOrder(request);
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

    private void ensureOrderNotExists(String orderNo) {
        Long count = thirdPartyPayOrderMapper.selectCount(
            new LambdaQueryWrapper<ThirdPartyPayOrder>()
                .eq(ThirdPartyPayOrder::getOrderNo, orderNo)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单已存在: " + orderNo);
        }
    }

    private void saveThirdPartyPayOrder(PaymentRequest request) {
        ThirdPartyPayOrder order = new ThirdPartyPayOrder();
        order.setOrderNo(request.getOrderId());
        order.setTargetId(request.getPlan().getId());
        order.setTargetType(TARGET_TYPE_PLAN);
        order.setPaymentMethodCode(request.getPaymentMethod());
        order.setOrderTitle(request.getPlan().getPlanName());
        order.setTotalAmount(request.getAmount());
        order.setStatus(0);
        order.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        order.setRemark("支付宝下单创建");
        thirdPartyPayOrderMapper.insert(order);
    }
}