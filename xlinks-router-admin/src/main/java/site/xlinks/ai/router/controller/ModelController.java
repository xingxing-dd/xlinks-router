package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ModelCreateDTO;
import site.xlinks.ai.router.dto.ModelUpdateDTO;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.service.ModelService;

/**
 * Model 管理接口
 */
@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
@Tag(name = "Model 管理", description = "Model 管理相关接口")
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    @Operation(summary = "新增 Model")
    public Result<Model> create(@RequestBody ModelCreateDTO dto) {
        Model model = new Model();
        model.setModelName(dto.getModelName());
        model.setModelCode(dto.getModelCode());
        model.setEndpointId(dto.getEndpointId());
        model.setProviderId(dto.getProviderId());
        model.setModelDesc(dto.getModelDesc());
        model.setInputPrice(dto.getInputPrice());
        model.setOutputPrice(dto.getOutputPrice());
        model.setContextSize(dto.getContextSize());
        model.setStatus(dto.getStatus());
        model.setRemark(dto.getRemark());
        
        modelService.save(model);
        return Result.success(model);
    }

    @GetMapping
    @Operation(summary = "Model 列表")
    public Result<PageResult<Model>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long endpointId,
            @RequestParam(required = false) String modelCode,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = modelService.pageQuery(page, pageSize, providerId, endpointId, modelCode, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Model 详情")
    public Result<Model> get(@PathVariable Long id) {
        return Result.success(modelService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Model")
    public Result<Void> update(@PathVariable Long id, @RequestBody ModelUpdateDTO dto) {
        Model model = new Model();
        model.setId(id);
        if (dto.getModelName() != null) {
            model.setModelName(dto.getModelName());
        }
        if (dto.getModelDesc() != null) {
            model.setModelDesc(dto.getModelDesc());
        }
        if (dto.getInputPrice() != null) {
            model.setInputPrice(dto.getInputPrice());
        }
        if (dto.getOutputPrice() != null) {
            model.setOutputPrice(dto.getOutputPrice());
        }
        if (dto.getContextSize() != null) {
            model.setContextSize(dto.getContextSize());
        }
        if (dto.getRemark() != null) {
            model.setRemark(dto.getRemark());
        }
        
        modelService.update(model);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Model")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        modelService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Model")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.deleteById(id);
        return Result.success();
    }
}
