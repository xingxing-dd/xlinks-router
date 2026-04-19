package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.token.CreateCustomerTokenRequest;
import site.xlinks.ai.router.client.dto.token.CreateCustomerTokenResponse;
import site.xlinks.ai.router.client.dto.token.CustomerTokenItemResponse;
import site.xlinks.ai.router.client.dto.token.CustomerTokenSummaryResponse;
import site.xlinks.ai.router.client.dto.token.RefreshCustomerTokenResponse;
import site.xlinks.ai.router.client.dto.token.UpdateCustomerTokenRequest;
import site.xlinks.ai.router.client.service.CustomerTokenService;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.UsageRecordMapper;
import site.xlinks.ai.router.model.TokenUsageStats;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer-tokens")
@RequiredArgsConstructor
public class CustomerTokenController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerTokenService customerTokenService;
    private final UsageRecordMapper usageRecordMapper;

    @GetMapping("/summary")
    public Result<CustomerTokenSummaryResponse> getTokenSummary() {
        CustomerAccount account = CustomerAccountContext.getAccount();
        var tokenPage = customerTokenService.pageTokens(account.getId(), 1, 500);
        List<CustomerToken> tokens = tokenPage.getRecords();
        List<TokenUsageStats> usageStats = usageRecordMapper.aggregateTokenStatsByAccountId(account.getId());

        long totalRequests = usageStats.stream()
                .map(TokenUsageStats::getTotalRequests)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .sum();

        LocalDateTime now = LocalDateTime.now();
        int activeTokens = 0;
        int disabledTokens = 0;
        int expiredTokens = 0;
        for (CustomerToken token : tokens) {
            if (token.getExpireTime() != null && now.isAfter(token.getExpireTime())) {
                expiredTokens++;
            } else if (token.getStatus() != null && token.getStatus() == 1) {
                activeTokens++;
            } else {
                disabledTokens++;
            }
        }

        return Result.success(new CustomerTokenSummaryResponse(
                tokens.size(),
                activeTokens,
                disabledTokens,
                expiredTokens,
                totalRequests
        ));
    }

    @GetMapping
    public Result<PageResult<CustomerTokenItemResponse>> getTokens(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        var tokenPage = customerTokenService.pageTokens(account.getId(), page, pageSize);
        Map<String, TokenUsageStats> usageStatsMap = toUsageStatsMap(account.getId());

        List<CustomerTokenItemResponse> records = tokenPage.getRecords().stream()
                .map(token -> toItemResponse(token, usageStatsMap.get(token.getTokenValue())))
                .toList();
        return Result.success(PageResult.of(records, tokenPage.getTotal(), page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<CustomerTokenItemResponse> getToken(@PathVariable Long id) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        CustomerToken token = customerTokenService.getToken(account.getId(), id);
        Map<String, TokenUsageStats> usageStatsMap = toUsageStatsMap(account.getId());
        return Result.success(toItemResponse(token, usageStatsMap.get(token.getTokenValue())));
    }

    @PostMapping
    public Result<CreateCustomerTokenResponse> createToken(@Valid @RequestBody CreateCustomerTokenRequest request) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        CustomerToken token = customerTokenService.createToken(account.getId(), account.getUsername(), request, account.getUsername());

        CreateCustomerTokenResponse response = new CreateCustomerTokenResponse();
        response.setId(String.valueOf(token.getId()));
        response.setTokenName(token.getTokenName());
        response.setTokenValue(token.getTokenValue());
        response.setExpireTime(formatDateTime(token.getExpireTime()));
        response.setDailyQuota(token.getDailyQuota());
        response.setTotalQuota(token.getTotalQuota());
        response.setCreatedAt(formatDateTime(token.getCreatedAt()));
        return Result.success(response);
    }

    @PutMapping("/{id}")
    public Result<Void> updateToken(@PathVariable Long id, @RequestBody UpdateCustomerTokenRequest request) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        customerTokenService.updateToken(account.getId(), id, request, account.getUsername());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateCustomerTokenRequest request) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("status不能为空");
        }
        customerTokenService.updateStatus(account.getId(), id, request.getStatus(), account.getUsername());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteToken(@PathVariable Long id) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        customerTokenService.deleteToken(account.getId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/refresh")
    public Result<RefreshCustomerTokenResponse> refreshToken(@PathVariable Long id) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        CustomerToken token = customerTokenService.refreshToken(account.getId(), id, account.getUsername());
        RefreshCustomerTokenResponse response = new RefreshCustomerTokenResponse();
        response.setTokenValue(token.getTokenValue());
        return Result.success(response);
    }

    private CustomerTokenItemResponse toItemResponse(CustomerToken token, TokenUsageStats usageStats) {
        return new CustomerTokenItemResponse(
                String.valueOf(token.getId()),
                token.getCustomerName(),
                token.getTokenName(),
                token.getTokenValue(),
                token.getStatus(),
                formatDateTime(token.getExpireTime()),
                parseAllowedModels(token.getAllowedModels()),
                token.getDailyQuota(),
                defaultDecimal(token.getUsedQuota()),
                token.getTotalQuota(),
                defaultDecimal(token.getTotalUsedQuota()),
                usageStats == null || usageStats.getTotalRequests() == null ? 0 : usageStats.getTotalRequests().intValue(),
                formatDateTime(usageStats == null ? null : usageStats.getLastUsedAt()),
                formatDateTime(token.getCreatedAt())
        );
    }

    private Map<String, TokenUsageStats> toUsageStatsMap(Long accountId) {
        List<TokenUsageStats> usageStats = usageRecordMapper.aggregateTokenStatsByAccountId(accountId);
        Map<String, TokenUsageStats> usageStatsMap = new HashMap<>();
        for (TokenUsageStats item : usageStats) {
            if (item != null && item.getCustomerToken() != null && !item.getCustomerToken().isBlank()) {
                usageStatsMap.put(item.getCustomerToken(), item);
            }
        }
        return usageStatsMap;
    }

    private String formatDateTime(java.time.LocalDateTime time) {
        return time == null ? null : time.format(DATETIME_FORMATTER);
    }

    private List<String> parseAllowedModels(String allowedModels) {
        if (allowedModels == null || allowedModels.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(allowedModels, List.class);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
