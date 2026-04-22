package site.xlinks.ai.router.client.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.user.UserInfoResponse;
import site.xlinks.ai.router.client.dto.user.UserSubscriptionResponse;
import site.xlinks.ai.router.client.service.CustomerAccountService;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerAccountService customerAccountService;
    private final WalletService walletService;

    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        Long accountId = CustomerAccountContext.requireAccount().getId();
        CustomerAccount account = customerAccountService.getById(accountId);

        UserInfoResponse response = new UserInfoResponse();
        if (account == null) {
            return Result.success(response);
        }
        response.setId(account.getId());
        response.setEmail(account.getEmail());
        response.setNickname(account.getUsername());
        response.setAvatar(null);
        response.setBalance(walletService.ensureWallet(accountId).getMainWallet().getAvailableBalance());
        response.setStatus(account.getStatus());
        response.setCreatedAt(account.getCreatedAt() == null ? null : account.getCreatedAt().format(DATE_TIME_FORMATTER));
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
