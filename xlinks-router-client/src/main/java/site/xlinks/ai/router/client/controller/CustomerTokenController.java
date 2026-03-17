package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.token.CreateCustomerTokenRequest;
import site.xlinks.ai.router.client.dto.token.CreateCustomerTokenResponse;
import site.xlinks.ai.router.client.dto.token.CustomerTokenItemResponse;
import site.xlinks.ai.router.client.dto.token.RefreshCustomerTokenResponse;
import site.xlinks.ai.router.client.dto.token.UpdateCustomerTokenRequest;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-tokens")
public class CustomerTokenController {

    @GetMapping
    public Result<PageResult<CustomerTokenItemResponse>> getTokens(@RequestParam(defaultValue = "1") Integer page,
                                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        List<CustomerTokenItemResponse> records = List.of(
                new CustomerTokenItemResponse(1L, "张三", "生产环境主Key", "sk-abc123***pqr678", 1, "2027-01-01 00:00:00", List.of("claude-sonnet", "gpt-4"), 12453, "2026-03-17 14:30:00", "2026-02-15 10:30:00"),
                new CustomerTokenItemResponse(2L, "张三", "测试环境Key", "sk-test789***pqr678", 1, "2027-02-01 00:00:00", List.of("claude-haiku"), 3241, "2026-03-17 13:20:00", "2026-03-01 09:00:00"),
                new CustomerTokenItemResponse(3L, "张三", "开发环境Key", "sk-dev456***efg789", 0, "2027-03-01 00:00:00", List.of("claude-opus"), 856, "2026-03-12 10:00:00", "2026-03-05 18:20:00")
        );
        return Result.success(PageResult.of(records, records.size(), page, pageSize));
    }

    @PostMapping
    public Result<CreateCustomerTokenResponse> createToken(@Valid @RequestBody CreateCustomerTokenRequest request) {
        CreateCustomerTokenResponse response = new CreateCustomerTokenResponse();
        response.setId(4L);
        response.setTokenName(request.getTokenName());
        response.setTokenValue("sk-newabc123def456ghi789jkl012mno345pqr");
        response.setExpireTime("2027-03-17 00:00:00");
        response.setCreatedAt("2026-03-17 16:00:00");
        return Result.success(response);
    }

    @PutMapping("/{id}")
    public Result<Void> updateToken(@PathVariable Long id, @RequestBody UpdateCustomerTokenRequest request) {
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteToken(@PathVariable Long id) {
        return Result.success();
    }

    @PostMapping("/{id}/refresh")
    public Result<RefreshCustomerTokenResponse> refreshToken(@PathVariable Long id) {
        RefreshCustomerTokenResponse response = new RefreshCustomerTokenResponse();
        response.setTokenValue("sk-refreshedabc123def456ghi789jkl012mno");
        return Result.success(response);
    }
}
