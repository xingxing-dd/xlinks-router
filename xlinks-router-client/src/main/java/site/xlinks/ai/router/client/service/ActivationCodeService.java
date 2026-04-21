package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.plan.ActivationCodeConsumeResponse;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ActivationCodeStock;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.ActivationCodeStockMapper;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 激活码兑换服务
 */
@Service
@RequiredArgsConstructor
public class ActivationCodeService {

    private static final int STATUS_AVAILABLE = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_USED = 2;
    private static final String ORDER_TYPE_SUBSCRIPTION_PURCHASE = "subscription_purchase";
    private static final String PAYMENT_CHANNEL_THIRD_PARTY = "third-party";
    private static final int ORDER_STATUS_PAID = 1;
    private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ActivationCodeStockMapper activationCodeStockMapper;
    private final PlanMapper planMapper;
    private final CustomerPlanMapper customerPlanMapper;
    private final CustomerOrderMapper customerOrderMapper;
    private final OrderFulfillmentService orderFulfillmentService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public ActivationCodeConsumeResponse consume(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码不能为空");
        }

        CustomerAccount account = CustomerAccountContext.getAccount();
        if (account == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String normalizedCode = code.trim();

        LambdaQueryWrapper<ActivationCodeStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivationCodeStock::getActivationCode, normalizedCode);
        ActivationCodeStock stock = activationCodeStockMapper.selectOne(wrapper);
        if (stock == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码不存在或不可用");
        }

        if (stock.getStatus() != null && stock.getStatus() == STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码不可用");
        }

        if (stock.getStatus() != null && stock.getStatus() == STATUS_USED) {
            if (stock.getUsedBy() != null && stock.getUsedBy().equals(account.getId())) {
                return buildResponseFromExisting(stock);
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, "激活码已被使用");
        }

        Plan plan = planMapper.selectById(stock.getPlanId());
        if (plan == null || plan.getStatus() == null || plan.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "套餐不存在或已下架");
        }

        CustomerOrder order = buildActivationOrder(account, stock, plan);
        customerOrderMapper.insert(order);

        OrderFulfillmentResult fulfillmentResult = orderFulfillmentService.handlePaidOrder(order.getOrderNo());
        if (fulfillmentResult.getCustomerPlanId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单履约失败，请稍后重试");
        }
        CustomerPlan customerPlan = customerPlanMapper.selectById(fulfillmentResult.getCustomerPlanId());
        if (customerPlan == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单履约失败，请稍后重试");
        }

        stock.setStatus(STATUS_USED);
        stock.setUsedAt(LocalDateTime.now());
        stock.setUsedBy(account.getId());
        stock.setSubscriptionId(customerPlan.getId());
        stock.setOrderId(order.getOrderNo());
        stock.setUpdateBy(String.valueOf(account.getId()));
        activationCodeStockMapper.updateById(stock);

        ActivationCodeConsumeResponse response = new ActivationCodeConsumeResponse();
        response.setMessage("激活成功");
        response.setActivatedPlanId(String.valueOf(plan.getId()));
        response.setActivatedPlanName(plan.getPlanName());
        response.setExpireTime(customerPlan.getPlanExpireTime().toString().replace('T', ' '));
        response.setSubscriptionId(String.valueOf(customerPlan.getId()));
        return response;
    }

    private ActivationCodeConsumeResponse buildResponseFromExisting(ActivationCodeStock stock) {
        ActivationCodeConsumeResponse response = new ActivationCodeConsumeResponse();
        Plan plan = planMapper.selectById(stock.getPlanId());
        response.setMessage("激活成功");
        if (plan != null) {
            response.setActivatedPlanId(String.valueOf(plan.getId()));
            response.setActivatedPlanName(plan.getPlanName());
        }
        CustomerPlan customerPlan = null;
        if (stock.getSubscriptionId() != null) {
            customerPlan = customerPlanMapper.selectById(stock.getSubscriptionId());
        }
        response.setExpireTime(customerPlan != null && customerPlan.getPlanExpireTime() != null
                ? customerPlan.getPlanExpireTime().toString().replace('T', ' ')
                : (stock.getUsedAt() != null ? stock.getUsedAt().toString().replace('T', ' ') : null));
        response.setSubscriptionId(stock.getSubscriptionId() != null ? String.valueOf(stock.getSubscriptionId()) : null);
        return response;
    }

    private CustomerOrder buildActivationOrder(CustomerAccount account, ActivationCodeStock stock, Plan plan) {
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(generateActivationOrderNo(stock.getId()));
        order.setRefNo("activation:" + stock.getActivationCode());
        order.setAccountId(account.getId());
        order.setOrderType(ORDER_TYPE_SUBSCRIPTION_PURCHASE);
        order.setOrderTitle("激活码兑换-" + plan.getPlanName());
        order.setOrderInfo(buildActivationOrderInfo(stock, plan));
        order.setPaymentChannel(PAYMENT_CHANNEL_THIRD_PARTY);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(ORDER_STATUS_PAID);
        order.setCompleteAt(now);
        order.setExpiredAt(now);
        order.setRemark("激活码兑换自动支付");
        order.setCreateBy(String.valueOf(account.getId()));
        order.setUpdateBy(String.valueOf(account.getId()));
        return order;
    }

    private String buildActivationOrderInfo(ActivationCodeStock stock, Plan plan) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("activationCode", stock.getActivationCode());
            snapshot.put("planId", plan.getId());
            snapshot.put("planName", plan.getPlanName());
            snapshot.put("durationDays", plan.getDurationDays());
            snapshot.put("dailyQuota", plan.getDailyQuota());
            snapshot.put("totalQuota", plan.getTotalQuota());
            snapshot.put("multiplier", plan.getMultiplier());
            snapshot.put("source", "activation_code");
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String generateActivationOrderNo(Long stockId) {
        return "ACT" + stockId + ORDER_NO_TIME_FORMATTER.format(LocalDateTime.now());
    }
}

