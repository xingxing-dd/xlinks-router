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
import site.xlinks.ai.router.dto.ActivationCodeGenerateDTO;
import site.xlinks.ai.router.dto.ActivationCodeUpdateDTO;
import site.xlinks.ai.router.service.ActivationCodeStockService;
import site.xlinks.ai.router.vo.ActivationCodeGenerateVO;
import site.xlinks.ai.router.vo.ActivationCodeVO;

/**
 * Activation code management API.
 */
@RestController
@RequestMapping("/api/activation-codes")
@RequiredArgsConstructor
@Tag(name = "Activation Code Management", description = "Plan activation code management APIs")
public class ActivationCodeController {

    private final ActivationCodeStockService activationCodeStockService;

    @PostMapping("/generate")
    @Operation(summary = "Batch generate activation codes")
    public Result<ActivationCodeGenerateVO> generate(@Valid @RequestBody ActivationCodeGenerateDTO dto) {
        return Result.success(activationCodeStockService.generate(dto));
    }

    @GetMapping
    @Operation(summary = "Activation code list")
    public Result<PageResult<ActivationCodeVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) Long planId,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) String activationCode,
                                                     @RequestParam(required = false) String usedAccount,
                                                     @RequestParam(required = false) Long subscriptionId,
                                                     @RequestParam(required = false) String orderId) {
        var pageResult = activationCodeStockService.pageQuery(
                page,
                pageSize,
                planId,
                status,
                activationCode,
                usedAccount,
                subscriptionId,
                orderId
        );
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Activation code detail")
    public Result<ActivationCodeVO> get(@PathVariable Long id) {
        return Result.success(activationCodeStockService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update activation code")
    public Result<ActivationCodeVO> update(@PathVariable Long id, @Valid @RequestBody ActivationCodeUpdateDTO dto) {
        return Result.success(activationCodeStockService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable activation code")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        activationCodeStockService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activation code")
    public Result<Void> delete(@PathVariable Long id) {
        activationCodeStockService.deleteById(id);
        return Result.success();
    }
}
