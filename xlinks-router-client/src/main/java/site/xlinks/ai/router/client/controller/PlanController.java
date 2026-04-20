package site.xlinks.ai.router.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.plan.ActiveSubscriptionResponse;
import site.xlinks.ai.router.client.dto.plan.ActivationCodeConsumeRequest;
import site.xlinks.ai.router.client.dto.plan.ActivationCodeConsumeResponse;
import site.xlinks.ai.router.client.dto.plan.CreateOrderRequest;
import site.xlinks.ai.router.client.dto.plan.CreateOrderResponse;
import site.xlinks.ai.router.client.dto.plan.HistoricalSubscriptionResponse;
import site.xlinks.ai.router.client.dto.plan.PlanItemResponse;
import site.xlinks.ai.router.client.dto.plan.RechargeOptionResponse;
import site.xlinks.ai.router.client.service.ActivationCodeService;
import site.xlinks.ai.router.client.service.PlanOrderService;
import site.xlinks.ai.router.client.service.PlanService;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlanController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final PlanService planService;
    private final PlanOrderService planOrderService;
    private final ActivationCodeService activationCodeService;
    private final CustomerPlanMapper customerPlanMapper;

    @GetMapping("/plans")
    public Result<List<PlanItemResponse>> getPlans() {
        Long accountId = CustomerAccountContext.getAccountId();
        List<Plan> plans = planService.listVisiblePlans(accountId);
        List<PlanItemResponse> responses = plans.stream()
                .map(this::toPlanResponse)
                .toList();
        return Result.success(responses);
    }

    @GetMapping("/subscriptions/active")
    public Result<List<ActiveSubscriptionResponse>> getActiveSubscriptions() {
        Long accountId = CustomerAccountContext.getAccountId();
        if (accountId == null) {
            return Result.success(List.of());
        }
        List<CustomerPlan> plans = customerPlanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CustomerPlan>()
                        .eq(CustomerPlan::getAccountId, accountId)
                        .eq(CustomerPlan::getStatus, 1)
                        .and(wrapper -> wrapper.isNull(CustomerPlan::getPlanExpireTime)
                                .or()
                                .gt(CustomerPlan::getPlanExpireTime, LocalDateTime.now()))
                        .orderByAsc(CustomerPlan::getPlanExpireTime)
        );
        LocalDateTime now = LocalDateTime.now();
        List<ActiveSubscriptionResponse> responses = plans.stream()
                .map(plan -> toActiveSubscriptionResponse(plan, now))
                .toList();
        return Result.success(responses);
    }

    @GetMapping("/subscriptions/history")
    public Result<List<HistoricalSubscriptionResponse>> getHistoricalSubscriptions() {
        Long accountId = CustomerAccountContext.getAccountId();
        if (accountId == null) {
            return Result.success(List.of());
        }
        List<CustomerPlan> plans = customerPlanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CustomerPlan>()
                        .eq(CustomerPlan::getAccountId, accountId)
                        .orderByDesc(CustomerPlan::getPlanExpireTime)
        );
        LocalDateTime now = LocalDateTime.now();
        List<HistoricalSubscriptionResponse> responses = plans.stream()
                .map(plan -> toHistoricalSubscriptionResponse(plan, now))
                .toList();
        return Result.success(responses);
    }

    @PostMapping("/activation-codes/consume")
    public Result<ActivationCodeConsumeResponse> consumeActivationCode(@Valid @RequestBody ActivationCodeConsumeRequest request) {
        return Result.success(activationCodeService.consume(request.getCode()));
    }

    @GetMapping("/recharge-options")
    public Result<List<RechargeOptionResponse>> getRechargeOptions() {
        return Result.success(List.of(
                new RechargeOptionResponse(new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("0")),
                new RechargeOptionResponse(new BigDecimal("200"), new BigDecimal("40"), new BigDecimal("0")),
                new RechargeOptionResponse(new BigDecimal("500"), new BigDecimal("100"), new BigDecimal("10")),
                new RechargeOptionResponse(new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("30")),
                new RechargeOptionResponse(new BigDecimal("2000"), new BigDecimal("400"), new BigDecimal("80")),
                new RechargeOptionResponse(new BigDecimal("5000"), new BigDecimal("1000"), new BigDecimal("250"))
        ));
    }

    @PostMapping("/orders")
    public Result<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long accountId = CustomerAccountContext.getAccountId();
        var paymentResult = planOrderService.createOrder(request.getPlanId(), request.getPaymentMethod(), accountId);
        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(paymentResult.getOrderId());
        response.setPayUrl(paymentResult.getPayUrl());
        response.setExpireTime(planOrderService.formatExpireTime(paymentResult.getExpiredAt()));
        return Result.success(response);
    }

    private PlanItemResponse toPlanResponse(Plan plan) {
        PlanItemResponse response = new PlanItemResponse();
        response.setId(String.valueOf(plan.getId()));
        response.setName(plan.getPlanName());
        response.setPrice(plan.getPrice());
        response.setDailyLimit(toBigDecimal(plan.getDailyQuota()));
        response.setMonthlyQuota(toBigDecimal(plan.getTotalQuota()));
        response.setDurationDays(plan.getDurationDays());
        response.setAllowedModels(parseAllowedModels(plan.getAllowedModels()));
        response.setCarryOverDailyQuota(Boolean.TRUE);
        response.setStackQuotaOnly(Boolean.TRUE);
        response.setRecommended(false);
        return response;
    }

    private List<String> parseAllowedModels(String allowedModels) {
        if (allowedModels == null || allowedModels.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(allowedModels, STRING_LIST_TYPE);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private ActiveSubscriptionResponse toActiveSubscriptionResponse(CustomerPlan plan, LocalDateTime now) {
        ActiveSubscriptionResponse response = new ActiveSubscriptionResponse();
        response.setId(String.valueOf(plan.getId()));
        response.setPlanId(String.valueOf(plan.getPlanId()));
        response.setPlanName(plan.getPlanName());
        response.setPurchaseDate(formatDateTime(plan.getCreatedAt()));
        response.setExpiryDate(formatDateTime(plan.getPlanExpireTime()));
        response.setTotalQuota(toBigDecimal(plan.getDailyQuota()));
        if (plan.getQuotaRefreshTime() != null && plan.getQuotaRefreshTime().getDayOfMonth() == LocalDateTime.now().getDayOfMonth()) {
            response.setDailyReset(true);
        } else {
            response.setDailyReset(false);
        }

        BigDecimal dailyQuota = defaultDecimal(plan.getDailyQuota());
        BigDecimal usedQuota = defaultDecimal(plan.getUsedQuota());
        BigDecimal remaining = dailyQuota.subtract(usedQuota);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        response.setRemainingQuota(remaining);

        if (plan.getPlanExpireTime() != null) {
            long daysRemaining = ChronoUnit.DAYS.between(now, plan.getPlanExpireTime());
            response.setDaysRemaining(Math.max((int) daysRemaining, 0));
        } else {
            response.setRemainingQuota(BigDecimal.ZERO);
        }

        response.setUsedPercentage(calculateUsedPercentage(usedQuota, dailyQuota));
        return response;
    }

    private HistoricalSubscriptionResponse toHistoricalSubscriptionResponse(CustomerPlan plan, LocalDateTime now) {
        HistoricalSubscriptionResponse response = new HistoricalSubscriptionResponse();
        response.setId(String.valueOf(plan.getId()));
        response.setPlanId(String.valueOf(plan.getPlanId()));
        response.setPlanName(plan.getPlanName());
        response.setPurchaseDate(formatDateTime(plan.getCreatedAt()));
        response.setExpiryDate(formatDateTime(plan.getPlanExpireTime()));
        response.setTotalQuota(toBigDecimal(plan.getTotalQuota()));

        BigDecimal totalUsed = plan.getTotalUsedQuota() == null ? defaultDecimal(plan.getUsedQuota()) : plan.getTotalUsedQuota();
        if (totalUsed.compareTo(BigDecimal.ZERO) < 0) {
            totalUsed = BigDecimal.ZERO;
        }
        response.setUsedQuota(totalUsed);
        response.setUsedPercentage(calculateOpenedPercentage(plan));
        response.setStatus(resolveHistoryStatus(plan, now));
        return response;
    }

    private String formatDateTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(time);
    }

    private BigDecimal toBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer calculateUsedPercentage(BigDecimal usedQuota, BigDecimal dailyQuota) {
        if (dailyQuota == null || dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal ratio = usedQuota
                .multiply(new BigDecimal("100"))
                .divide(dailyQuota, 0, RoundingMode.HALF_UP);
        return ratio.intValue();
    }

    private Integer calculateOpenedPercentage(CustomerPlan plan) {
        BigDecimal ratio = (plan.getTotalUsedQuota() == null ? BigDecimal.ZERO : plan.getTotalUsedQuota())
                .multiply(new BigDecimal("100"))
                .divide(plan.getTotalQuota(), 0, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("100")) > 0) {
            return 100;
        }
        return ratio.intValue();
    }

    private String resolveHistoryStatus(CustomerPlan plan, LocalDateTime now) {
        if (plan.getPlanExpireTime() != null && plan.getPlanExpireTime().isBefore(now)) {
            return "expired";
        }
        if (plan.getStatus() == 1) {
            return "success";
        }
        return "cancelled";
    }
}

