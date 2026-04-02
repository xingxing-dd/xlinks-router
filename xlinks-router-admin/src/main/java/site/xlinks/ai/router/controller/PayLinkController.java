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
import site.xlinks.ai.router.dto.PayLinkCreateDTO;
import site.xlinks.ai.router.dto.PayLinkUpdateDTO;
import site.xlinks.ai.router.service.PayLinkService;
import site.xlinks.ai.router.vo.PayLinkVO;

@RestController
@RequestMapping("/admin/pay-links")
@RequiredArgsConstructor
@Tag(name = "Pay Link Management", description = "Third-party pay link APIs")
public class PayLinkController {

    private final PayLinkService payLinkService;

    @PostMapping
    @Operation(summary = "Create pay link")
    public Result<PayLinkVO> create(@Valid @RequestBody PayLinkCreateDTO dto) {
        return Result.success(payLinkService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Pay link list")
    public Result<PageResult<PayLinkVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) Long targetId,
                                              @RequestParam(required = false) String planName,
                                              @RequestParam(required = false) Integer status) {
        var pageResult = payLinkService.pageQuery(page, pageSize, targetId, planName, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pay link detail")
    public Result<PayLinkVO> get(@PathVariable Long id) {
        return Result.success(payLinkService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pay link")
    public Result<PayLinkVO> update(@PathVariable Long id, @Valid @RequestBody PayLinkUpdateDTO dto) {
        return Result.success(payLinkService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable pay link")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        payLinkService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pay link")
    public Result<Void> delete(@PathVariable Long id) {
        payLinkService.deleteById(id);
        return Result.success();
    }
}
