package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.CustomerModelCreateDTO;
import site.xlinks.ai.router.dto.CustomerModelUpdateDTO;
import site.xlinks.ai.router.entity.CustomerModel;
import site.xlinks.ai.router.service.CustomerModelService;

/**
 * Customer Model 管理接口
 */
@RestController
@RequestMapping("/admin/customer-models")
@RequiredArgsConstructor
@Tag(name = "Customer Model 管理", description = "Customer Model 管理相关接口")
public class CustomerModelController {

    private final CustomerModelService customerModelService;

    @PostMapping
    @Operation(summary = "新增 Customer Model")
    public Result<CustomerModel> create(@RequestBody CustomerModelCreateDTO dto) {
        CustomerModel model = new CustomerModel();
        model.setLogicModelCode(dto.getLogicModelCode());
        model.setLogicModelName(dto.getLogicModelName());
        model.setModelType(dto.getModelType());
        model.setStatus(dto.getStatus());
        model.setIsDefault(dto.getIsDefault());
        model.setRemark(dto.getRemark());
        
        customerModelService.save(model);
        return Result.success(model);
    }

    @GetMapping
    @Operation(summary = "Customer Model 列表")
    public Result<PageResult<CustomerModel>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String logicModelCode,
            @RequestParam(required = false) String logicModelName,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = customerModelService.pageQuery(page, pageSize, logicModelCode, logicModelName, modelType, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Customer Model 详情")
    public Result<CustomerModel> get(@PathVariable Long id) {
        return Result.success(customerModelService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Customer Model")
    public Result<Void> update(@PathVariable Long id, @RequestBody CustomerModelUpdateDTO dto) {
        CustomerModel model = new CustomerModel();
        model.setId(id);
        if (dto.getLogicModelName() != null) {
            model.setLogicModelName(dto.getLogicModelName());
        }
        if (dto.getIsDefault() != null) {
            model.setIsDefault(dto.getIsDefault());
        }
        if (dto.getRemark() != null) {
            model.setRemark(dto.getRemark());
        }
        
        customerModelService.update(model);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Customer Model")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        customerModelService.updateStatus(id, status);
        return Result.success();
    }
}
