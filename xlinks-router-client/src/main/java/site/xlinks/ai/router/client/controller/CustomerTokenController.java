package site.xlinks.ai.router.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import site.xlinks.ai.router.client.dto.token.RefreshCustomerTokenResponse;
import site.xlinks.ai.router.client.dto.token.UpdateCustomerTokenRequest;
import site.xlinks.ai.router.client.service.CustomerTokenService;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.UsageRecordMapper;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-tokens")
@RequiredArgsConstructor
public class CustomerTokenController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerTokenService customerTokenService;
    private final UsageRecordMapper usageRecordMapper;

    @GetMapping
    public Result<PageResult<CustomerTokenItemResponse>> getTokens(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        var tokenPage = customerTokenService.pageTokens(account.getId(), page, pageSize);

        List<CustomerTokenItemResponse> records = tokenPage.getRecords().stream()
                .map(token -> toItemResponse(token, account.getId()))
                .toList();
        return Result.success(PageResult.of(records, tokenPage.getTotal(), page, pageSize));
    }

    @PostMapping
    public Result<CreateCustomerTokenResponse> createToken(@Valid @RequestBody CreateCustomerTokenRequest request) {
        CustomerAccount account = CustomerAccountContext.getAccount();
        CustomerToken token = customerTokenService.createToken(account.getId(), account.getEmail(), request, account.getUsername());

        CreateCustomerTokenResponse response = new CreateCustomerTokenResponse();
        response.setId(String.valueOf(token.getId()));
        response.setTokenName(token.getTokenName());
        response.setTokenValue(token.getTokenValue());
        response.setExpireTime(formatDateTime(token.getExpireTime()));
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

    private CustomerTokenItemResponse toItemResponse(CustomerToken token, Long accountId) {
        Integer totalRequests = queryTotalRequests(accountId, token.getTokenValue());
        return new CustomerTokenItemResponse(
                String.valueOf(token.getId()),
                token.getCustomerName(),
                token.getTokenName(),
                token.getTokenValue(),
                token.getStatus(),
                formatDateTime(token.getExpireTime()),
                parseAllowedModels(token.getAllowedModels()),
                totalRequests,
                null,
                formatDateTime(token.getCreatedAt())
        );
    }

    private Integer queryTotalRequests(Long accountId, String tokenValue) {
        if (accountId == null || tokenValue == null || tokenValue.isBlank()) {
            return 0;
        }
        Long count = usageRecordMapper.selectCount(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, accountId)
                        .eq(UsageRecord::getCustomerToken, tokenValue)
        );
        return count == null ? 0 : count.intValue();
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

    private String maskToken(String tokenValue) {
        if (tokenValue == null || tokenValue.length() < 10) {
            return tokenValue;
        }
        return tokenValue.substring(0, 6) + "***" + tokenValue.substring(tokenValue.length() - 4);
    }
}
