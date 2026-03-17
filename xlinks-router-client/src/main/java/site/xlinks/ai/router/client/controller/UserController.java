package site.xlinks.ai.router.client.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.user.UserInfoResponse;
import site.xlinks.ai.router.client.dto.user.UserSubscriptionResponse;
import site.xlinks.ai.router.common.result.Result;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(1L);
        response.setEmail("user@example.com");
        response.setNickname("张三");
        response.setAvatar("https://example.com/avatar.jpg");
        response.setBalance(new BigDecimal("1258.00"));
        response.setStatus(1);
        response.setCreatedAt("2026-02-15 10:30:00");
        return Result.success(response);
    }

    @GetMapping("/subscription")
    public Result<UserSubscriptionResponse> getSubscription() {
        UserSubscriptionResponse response = new UserSubscriptionResponse();
        response.setPlanId("medium");
        response.setPlanName("Codex中包套餐");
        response.setDailyLimit(new BigDecimal("60"));
        response.setDailyUsed(new BigDecimal("25.5"));
        response.setMonthlyQuota(new BigDecimal("1800"));
        response.setMonthlyUsed(new BigDecimal("450"));
        response.setConcurrency(12);
        response.setCurrentConcurrency(3);
        response.setExpireTime("2026-04-17 23:59:59");
        response.setStatus("active");
        return Result.success(response);
    }
}
