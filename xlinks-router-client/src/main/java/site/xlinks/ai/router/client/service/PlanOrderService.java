package site.xlinks.ai.router.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.payment.PaymentRequest;
import site.xlinks.ai.router.client.payment.PaymentResult;
import site.xlinks.ai.router.client.payment.PaymentStrategy;
import site.xlinks.ai.router.client.payment.PaymentStrategyFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.mapper.PlanMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 套餐下单服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanOrderService {

    public static final int DEFAULT_ORDER_EXPIRE_MINUTES = 30;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlanMapper planMapper;
    private final PaymentStrategyFactory paymentStrategyFactory;

    public PaymentResult createOrder(String planId, String paymentMethod, Long accountId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null || plan.getStatus() == null || plan.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "套餐不存在或已下架");
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(paymentMethod);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付方式不支持");
        }

        String orderId = generateOrderId(plan.getId());
        log.info("购买套餐 orderId,{}", orderId);
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(DEFAULT_ORDER_EXPIRE_MINUTES);
        PaymentRequest request = new PaymentRequest(orderId, accountId, plan, plan.getPrice(), paymentMethod, expiredAt);
        return strategy.pay(request);
    }

    public String formatExpireTime(LocalDateTime expiredAt) {
        if (expiredAt == null) {
            return null;
        }
        return expiredAt.format(DATETIME_FORMATTER);
    }

    private String generateOrderId(Long planId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PLAN" + planId + timestamp;
    }
}
