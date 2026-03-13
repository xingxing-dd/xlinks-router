package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ProviderCreateDTO;
import site.xlinks.ai.router.dto.ProviderUpdateDTO;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.service.ProviderService;

/**
 * Provider 管理接口
 */
@RestController
@RequestMapping("/admin/providers")
@RequiredArgsConstructor
@Tag(name = "Provider 管理", description = "Provider 管理相关接口")
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    @Operation(summary = "新增 Provider")
    public Result<Provider> create(@RequestBody ProviderCreateDTO dto) {
        Provider provider = new Provider();
        provider.setProviderCode(dto.getProviderCode());
        provider.setProviderName(dto.getProviderName());
        provider.setProviderType(dto.getProviderType());
        provider.setBaseUrl(dto.getBaseUrl());
        provider.setStatus(dto.getStatus());
        provider.setRemark(dto.getRemark());
        
        providerService.save(provider);
        return Result.success(provider);
    }

    @GetMapping
    @Operation(summary = "Provider 列表")
    public Result<PageResult<Provider>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = providerService.pageQuery(page, pageSize, providerCode, providerName, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Provider 详情")
    public Result<Provider> get(@PathVariable Long id) {
        return Result.success(providerService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Provider")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProviderUpdateDTO dto) {
        Provider provider = new Provider();
        provider.setId(id);
        if (dto.getProviderName() != null) {
            provider.setProviderName(dto.getProviderName());
        }
        if (dto.getBaseUrl() != null) {
            provider.setBaseUrl(dto.getBaseUrl());
        }
        if (dto.getRemark() != null) {
            provider.setRemark(dto.getRemark());
        }
        
        providerService.update(provider);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Provider")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        providerService.updateStatus(id, status);
        return Result.success();
    }
}
