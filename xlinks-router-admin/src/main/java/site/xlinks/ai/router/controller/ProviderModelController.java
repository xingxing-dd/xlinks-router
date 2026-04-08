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
import site.xlinks.ai.router.dto.ProviderModelCreateDTO;
import site.xlinks.ai.router.dto.ProviderModelUpdateDTO;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.service.ProviderModelService;

/**
 * Provider model mapping management API.
 */
@RestController
@RequestMapping("/api/provider-models")
@RequiredArgsConstructor
@Tag(name = "Provider Model Management", description = "Provider model mapping management APIs")
public class ProviderModelController {

    private final ProviderModelService providerModelService;

    @PostMapping
    @Operation(summary = "Create provider model mapping")
    public Result<ProviderModel> create(@Valid @RequestBody ProviderModelCreateDTO dto) {
        ProviderModel providerModel = new ProviderModel();
        providerModel.setProviderId(dto.getProviderId());
        providerModel.setModelId(dto.getModelId());
        providerModel.setProviderModelCode(dto.getProviderModelCode());
        providerModel.setProviderModelName(dto.getProviderModelName());
        providerModel.setStatus(dto.getStatus());
        providerModel.setRemark(dto.getRemark());
        providerModelService.save(providerModel);
        return Result.success(providerModel);
    }

    @GetMapping
    @Operation(summary = "Provider model mapping list")
    public Result<PageResult<ProviderModel>> list(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) Long providerId,
                                                  @RequestParam(required = false) Long modelId,
                                                  @RequestParam(required = false) String providerModelCode,
                                                  @RequestParam(required = false) Integer status) {
        var pageResult = providerModelService.pageQuery(page, pageSize, providerId, modelId, providerModelCode, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Provider model mapping detail")
    public Result<ProviderModel> get(@PathVariable Long id) {
        return Result.success(providerModelService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update provider model mapping")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProviderModelUpdateDTO dto) {
        ProviderModel providerModel = new ProviderModel();
        providerModel.setId(id);
        if (dto.getProviderId() != null) {
            providerModel.setProviderId(dto.getProviderId());
        }
        if (dto.getModelId() != null) {
            providerModel.setModelId(dto.getModelId());
        }
        if (dto.getProviderModelCode() != null) {
            providerModel.setProviderModelCode(dto.getProviderModelCode());
        }
        if (dto.getProviderModelName() != null) {
            providerModel.setProviderModelName(dto.getProviderModelName());
        }
        if (dto.getStatus() != null) {
            providerModel.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            providerModel.setRemark(dto.getRemark());
        }
        providerModelService.update(providerModel);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable provider model mapping")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        providerModelService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete provider model mapping")
    public Result<Void> delete(@PathVariable Long id) {
        providerModelService.deleteById(id);
        return Result.success();
    }
}
