package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ProviderCreateDTO;
import site.xlinks.ai.router.dto.ProviderUpdateDTO;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.service.ProviderService;

/**
 * Provider management API.
 */
@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
@Tag(name = "Provider Management", description = "Provider management APIs")
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    @Operation(summary = "Create provider")
    public Result<Provider> create(@Valid @RequestBody ProviderCreateDTO dto) {
        Provider provider = new Provider();
        provider.setProviderCode(dto.getProviderCode());
        provider.setProviderName(dto.getProviderName());
        provider.setSupportedProtocols(dto.getSupportedProtocols());
        provider.setPriority(dto.getPriority());
        provider.setCacheHitStrategy(dto.getCacheHitStrategy());
        provider.setBaseUrl(dto.getBaseUrl());
        provider.setProviderLogo(dto.getProviderLogo());
        provider.setProviderWebsite(dto.getProviderWebsite());
        provider.setStatus(dto.getStatus());
        provider.setRemark(dto.getRemark());

        providerService.save(provider);
        return Result.success(provider);
    }

    @GetMapping
    @Operation(summary = "Provider list")
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
    @Operation(summary = "Provider detail")
    public Result<Provider> get(@PathVariable Long id) {
        return Result.success(providerService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update provider")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProviderUpdateDTO dto) {
        Provider provider = new Provider();
        provider.setId(id);
        if (dto.getProviderName() != null) {
            provider.setProviderName(dto.getProviderName());
        }
        if (dto.getBaseUrl() != null) {
            provider.setBaseUrl(dto.getBaseUrl());
        }
        if (dto.getSupportedProtocols() != null) {
            provider.setSupportedProtocols(dto.getSupportedProtocols());
        }
        if (dto.getPriority() != null) {
            provider.setPriority(dto.getPriority());
        }
        if (dto.getCacheHitStrategy() != null) {
            provider.setCacheHitStrategy(dto.getCacheHitStrategy());
        }
        if (dto.getProviderLogo() != null) {
            provider.setProviderLogo(dto.getProviderLogo());
        }
        if (dto.getProviderWebsite() != null) {
            provider.setProviderWebsite(dto.getProviderWebsite());
        }
        if (dto.getRemark() != null) {
            provider.setRemark(dto.getRemark());
        }

        providerService.update(provider);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable provider")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        providerService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete provider")
    public Result<Void> delete(@PathVariable Long id) {
        providerService.deleteById(id);
        return Result.success();
    }
}
