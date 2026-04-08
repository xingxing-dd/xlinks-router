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
import site.xlinks.ai.router.dto.ModelCreateDTO;
import site.xlinks.ai.router.dto.ModelUpdateDTO;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.service.ModelService;

/**
 * Standard model management API.
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "Model Management", description = "Standard model management APIs")
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    @Operation(summary = "Create model")
    public Result<Model> create(@Valid @RequestBody ModelCreateDTO dto) {
        Model model = new Model();
        model.setModelName(dto.getModelName());
        model.setModelCode(dto.getModelCode());
        model.setModelDesc(dto.getModelDesc());
        model.setInputPrice(dto.getInputPrice());
        model.setOutputPrice(dto.getOutputPrice());
        model.setCacheHitPrice(dto.getCacheHitPrice());
        model.setContextSize(dto.getContextSize());
        model.setStatus(dto.getStatus());
        model.setRemark(dto.getRemark());
        modelService.save(model);
        return Result.success(model);
    }

    @GetMapping
    @Operation(summary = "Model list")
    public Result<PageResult<Model>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String modelCode,
                                          @RequestParam(required = false) Integer status) {
        var pageResult = modelService.pageQuery(page, pageSize, modelCode, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Model detail")
    public Result<Model> get(@PathVariable Long id) {
        return Result.success(modelService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update model")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ModelUpdateDTO dto) {
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
        if (dto.getCacheHitPrice() != null) {
            model.setCacheHitPrice(dto.getCacheHitPrice());
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
    @Operation(summary = "Enable or disable model")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        modelService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.deleteById(id);
        return Result.success();
    }
}
