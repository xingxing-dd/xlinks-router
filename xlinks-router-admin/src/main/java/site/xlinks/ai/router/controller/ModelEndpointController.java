package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ModelEndpointCreateDTO;
import site.xlinks.ai.router.dto.ModelEndpointUpdateDTO;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.service.ModelEndpointService;

/**
 * Model Endpoint 管理接口
 */
@RestController
@RequestMapping("/admin/model-endpoints")
@RequiredArgsConstructor
@Tag(name = "Model Endpoint 管理", description = "Model Endpoint 管理相关接口")
public class ModelEndpointController {

    private final ModelEndpointService modelEndpointService;

    @PostMapping
    @Operation(summary = "新增 Model Endpoint")
    public Result<ModelEndpoint> create(@Valid @RequestBody ModelEndpointCreateDTO dto) {
        ModelEndpoint modelEndpoint = new ModelEndpoint();
        modelEndpoint.setEndpointCode(dto.getEndpointCode());
        modelEndpoint.setEndpointName(dto.getEndpointName());
        modelEndpoint.setEndpointUrl(dto.getEndpointUrl());
        modelEndpoint.setStatus(dto.getStatus());
        modelEndpoint.setRemark(dto.getRemark());
        modelEndpointService.save(modelEndpoint);
        return Result.success(modelEndpoint);
    }

    @GetMapping
    @Operation(summary = "Model Endpoint 列表")
    public Result<PageResult<ModelEndpoint>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String endpointName,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = modelEndpointService.pageQuery(page, pageSize, endpointName, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Model Endpoint 详情")
    public Result<ModelEndpoint> get(@PathVariable Long id) {
        return Result.success(modelEndpointService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Model Endpoint")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ModelEndpointUpdateDTO dto) {
        ModelEndpoint modelEndpoint = new ModelEndpoint();
        modelEndpoint.setId(id);
        if (dto.getEndpointCode() != null) {
            modelEndpoint.setEndpointCode(dto.getEndpointCode());
        }
        if (dto.getEndpointName() != null) {
            modelEndpoint.setEndpointName(dto.getEndpointName());
        }
        if (dto.getEndpointUrl() != null) {
            modelEndpoint.setEndpointUrl(dto.getEndpointUrl());
        }
        if (dto.getRemark() != null) {
            modelEndpoint.setRemark(dto.getRemark());
        }
        modelEndpointService.update(modelEndpoint);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Model Endpoint")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        modelEndpointService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Model Endpoint")
    public Result<Void> delete(@PathVariable Long id) {
        modelEndpointService.deleteById(id);
        return Result.success();
    }
}
