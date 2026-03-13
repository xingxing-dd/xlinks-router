package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.CustomerModelMappingCreateDTO;
import site.xlinks.ai.router.dto.CustomerModelMappingUpdateDTO;
import site.xlinks.ai.router.entity.CustomerModelMapping;
import site.xlinks.ai.router.service.CustomerModelMappingService;

/**
 * Customer Model Mapping 管理接口
 */
@RestController
@RequestMapping("/admin/model-mappings")
@RequiredArgsConstructor
@Tag(name = "Model Mapping 管理", description = "模型映射管理接口")
public class CustomerModelMappingController {

    private final CustomerModelMappingService relationService;

    @PostMapping
    @Operation(summary = "新增关联")
    public Result<CustomerModelMapping> create(@RequestBody CustomerModelMappingCreateDTO dto) {
        CustomerModelMapping relation = new CustomerModelMapping();
        relation.setCustomerModelId(dto.getCustomerModelId());
        relation.setProviderModelId(dto.getProviderModelId());
        relation.setPriority(dto.getPriority());
        relation.setStatus(dto.getStatus());
        relation.setRemark(dto.getRemark());
        
        relationService.save(relation);
        return Result.success(relation);
    }

    @GetMapping
    @Operation(summary = "关联列表")
    public Result<PageResult<CustomerModelMapping>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long customerModelId,
            @RequestParam(required = false) Long providerModelId,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = relationService.pageQuery(page, pageSize, customerModelId, providerModelId, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "关联详情")
    public Result<CustomerModelMapping> get(@PathVariable Long id) {
        return Result.success(relationService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新关联")
    public Result<Void> update(@PathVariable Long id, @RequestBody CustomerModelMappingUpdateDTO dto) {
        CustomerModelMapping relation = new CustomerModelMapping();
        relation.setId(id);
        if (dto.getPriority() != null) {
            relation.setPriority(dto.getPriority());
        }
        if (dto.getRemark() != null) {
            relation.setRemark(dto.getRemark());
        }
        
        relationService.update(relation);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用关联")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        relationService.updateStatus(id, status);
        return Result.success();
    }
}
