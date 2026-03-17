package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.plan.CreateOrderRequest;
import site.xlinks.ai.router.client.dto.plan.CreateOrderResponse;
import site.xlinks.ai.router.client.dto.plan.PlanItemResponse;
import site.xlinks.ai.router.client.dto.plan.RechargeOptionResponse;
import site.xlinks.ai.router.common.result.Result;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PlanController {

    @GetMapping("/plans")
    public Result<List<PlanItemResponse>> getPlans() {
        return Result.success(List.of(
                buildPlan("small", "Codex小包套餐", new BigDecimal("45.00"), 30, 900, 8, false),
                buildPlan("medium", "Codex中包套餐", new BigDecimal("60.00"), 60, 1800, 12, true),
                buildPlan("large", "Codex大包套餐", new BigDecimal("75.00"), 90, 2700, 16, false)
        ));
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
        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId("ORDER202603171500001");
        response.setPayUrl("https://pay.example.com/alipay?order_id=ORDER202603171500001");
        response.setExpireTime("2026-03-17 16:30:00");
        return Result.success(response);
    }

    private PlanItemResponse buildPlan(String id, String name, BigDecimal price, Integer dailyLimit,
                                       Integer monthlyQuota, Integer concurrency, Boolean recommended) {
        PlanItemResponse response = new PlanItemResponse();
        response.setId(id);
        response.setName(name);
        response.setPrice(price);
        response.setDailyLimit(new BigDecimal(String.valueOf(dailyLimit)));
        response.setMonthlyQuota(new BigDecimal(String.valueOf(monthlyQuota)));
        response.setConcurrency(concurrency);
        response.setFeatures(List.of(
                "有效期 30 天",
                "仅可用 Codex",
                "月度可用 $" + monthlyQuota + " 额度",
                "每日可用 $" + dailyLimit + " + 昨日未用完额度",
                "单套餐并发量为 " + concurrency,
                "套餐多买只叠加额度，不叠加时间"
        ));
        response.setRecommended(recommended);
        return response;
    }
}
