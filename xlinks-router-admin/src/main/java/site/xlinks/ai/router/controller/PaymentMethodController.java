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
import site.xlinks.ai.router.dto.PaymentMethodCreateDTO;
import site.xlinks.ai.router.dto.PaymentMethodUpdateDTO;
import site.xlinks.ai.router.service.PaymentMethodService;
import site.xlinks.ai.router.vo.PaymentMethodVO;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Method Management", description = "Payment method configuration APIs")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    @Operation(summary = "Create payment method")
    public Result<PaymentMethodVO> create(@Valid @RequestBody PaymentMethodCreateDTO dto) {
        return Result.success(paymentMethodService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Payment method list")
    public Result<PageResult<PaymentMethodVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String methodType,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer status) {
        var pageResult = paymentMethodService.pageQuery(page, pageSize, methodType, keyword, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Payment method detail")
    public Result<PaymentMethodVO> get(@PathVariable Long id) {
        return Result.success(paymentMethodService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment method")
    public Result<PaymentMethodVO> update(@PathVariable Long id, @Valid @RequestBody PaymentMethodUpdateDTO dto) {
        return Result.success(paymentMethodService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable payment method")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        paymentMethodService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment method")
    public Result<Void> delete(@PathVariable Long id) {
        paymentMethodService.deleteById(id);
        return Result.success();
    }
}
