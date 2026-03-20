package site.xlinks.ai.router.client.payment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ThirdPartyPayLink;
import site.xlinks.ai.router.mapper.ThirdPartyPayLinkMapper;

/**
 * 第三方支付策略
 */
@Component
@RequiredArgsConstructor
public class ThirdPartyPaymentStrategy implements PaymentStrategy {

    private static final String TARGET_TYPE_PLAN = "plan";

    private final ThirdPartyPayLinkMapper thirdPartyPayLinkMapper;

    @Override
    public String getMethod() {
        return "third-party";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        if (request.getPlan() == null || request.getPlan().getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "套餐信息缺失");
        }

        QueryWrapper<ThirdPartyPayLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("target_type", TARGET_TYPE_PLAN)
                .eq("target_id", request.getPlan().getId())
                .eq("status", 1)
                .last("limit 1");

        ThirdPartyPayLink payLink = thirdPartyPayLinkMapper.selectOne(queryWrapper);
        if (payLink == null || payLink.getPayUrl() == null || payLink.getPayUrl().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付链接不存在");
        }

        return new PaymentResult(request.getOrderId(), payLink.getPayUrl());
    }
}
