package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
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
    public Result<ModelEndpoint> create(@RequestBody ModelEndpoint modelEndpoint) {
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
    public Result<Void> update(@PathVariable Long id, @RequestBody ModelEndpoint modelEndpoint) {
        modelEndpoint.setId(id);
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
