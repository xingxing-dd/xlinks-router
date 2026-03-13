package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ProviderModelCreateDTO;
import site.xlinks.ai.router.dto.ProviderModelUpdateDTO;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.service.ProviderModelService;

/**
 * Provider Model 管理接口
 */
@RestController
@RequestMapping("/admin/provider-models")
@RequiredArgsConstructor
@Tag(name = "Provider Model 管理", description = "Provider Model 管理相关接口")
public class ProviderModelController {

    private final ProviderModelService providerModelService;

    @PostMapping
    @Operation(summary = "新增 Provider Model")
    public Result<ProviderModel> create(@RequestBody ProviderModelCreateDTO dto) {
        ProviderModel model = new ProviderModel();
        model.setProviderId(dto.getProviderId());
        model.setProviderModelCode(dto.getProviderModelCode());
        model.setProviderModelName(dto.getProviderModelName());
        model.setStatus(dto.getStatus());
        model.setRemark(dto.getRemark());
        
        providerModelService.save(model);
        return Result.success(model);
    }

    @GetMapping
    @Operation(summary = "Provider Model 列表")
    public Result<PageResult<ProviderModel>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String providerModelCode,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = providerModelService.pageQuery(page, pageSize, providerId, providerModelCode, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Provider Model 详情")
    public Result<ProviderModel> get(@PathVariable Long id) {
        return Result.success(providerModelService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Provider Model")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProviderModelUpdateDTO dto) {
        ProviderModel model = new ProviderModel();
        model.setId(id);
        if (dto.getProviderModelName() != null) {
            model.setProviderModelName(dto.getProviderModelName());
        }
        if (dto.getRemark() != null) {
            model.setRemark(dto.getRemark());
        }
        
        providerModelService.update(model);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Provider Model")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        providerModelService.updateStatus(id, status);
        return Result.success();
    }
}
