package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ProviderTokenCreateDTO;
import site.xlinks.ai.router.dto.ProviderTokenUpdateDTO;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.ProviderTokenService;

/**
 * Provider Token 管理接口
 */
@RestController
@RequestMapping("/admin/provider-tokens")
@RequiredArgsConstructor
@Tag(name = "Provider Token 管理", description = "Provider Token 管理相关接口")
public class ProviderTokenController {

    private final ProviderTokenService providerTokenService;

    @PostMapping
    @Operation(summary = "新增 Provider Token")
    public Result<ProviderToken> create(@RequestBody ProviderTokenCreateDTO dto) {
        ProviderToken token = new ProviderToken();
        token.setProviderId(dto.getProviderId());
        token.setTokenName(dto.getTokenName());
        token.setTokenValue(dto.getTokenValue()); // 注意：生产环境应加密存储
        token.setTokenStatus(dto.getTokenStatus());
        token.setQuotaTotal(dto.getQuotaTotal());
        token.setExpireTime(dto.getExpireTime());
        token.setRemark(dto.getRemark());
        
        providerTokenService.save(token);
        // 不返回 tokenValue
        token.setTokenValue(null);
        return Result.success(token);
    }

    @GetMapping
    @Operation(summary = "Provider Token 列表")
    public Result<PageResult<ProviderToken>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Integer tokenStatus) {
        
        var pageResult = providerTokenService.pageQuery(page, pageSize, providerId, tokenStatus);
        // 不返回 tokenValue
        pageResult.getRecords().forEach(t -> t.setTokenValue(null));
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Provider Token 详情")
    public Result<ProviderToken> get(@PathVariable Long id) {
        ProviderToken token = providerTokenService.getById(id);
        token.setTokenValue(null); // 不返回 tokenValue
        return Result.success(token);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Provider Token")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProviderTokenUpdateDTO dto) {
        ProviderToken token = new ProviderToken();
        token.setId(id);
        if (dto.getTokenName() != null) {
            token.setTokenName(dto.getTokenName());
        }
        if (dto.getTokenValue() != null) {
            token.setTokenValue(dto.getTokenValue());
        }
        if (dto.getQuotaTotal() != null) {
            token.setQuotaTotal(dto.getQuotaTotal());
        }
        if (dto.getExpireTime() != null) {
            token.setExpireTime(dto.getExpireTime());
        }
        if (dto.getRemark() != null) {
            token.setRemark(dto.getRemark());
        }
        
        providerTokenService.update(token);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Provider Token")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        providerTokenService.updateStatus(id, status);
        return Result.success();
    }
}
