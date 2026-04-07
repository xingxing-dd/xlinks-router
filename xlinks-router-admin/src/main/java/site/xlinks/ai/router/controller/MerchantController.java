package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.MerchantUpdateDTO;
import site.xlinks.ai.router.service.MerchantService;
import site.xlinks.ai.router.vo.MerchantVO;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "Merchant account management APIs")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    @Operation(summary = "Merchant list")
    public Result<PageResult<MerchantVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        var pageResult = merchantService.pageQuery(page, pageSize, keyword, status);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merchant detail")
    public Result<MerchantVO> get(@PathVariable Long id) {
        return Result.success(merchantService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update merchant")
    public Result<MerchantVO> update(@PathVariable Long id, @Valid @RequestBody MerchantUpdateDTO dto) {
        return Result.success(merchantService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable merchant")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.updateStatus(id, status);
        return Result.success();
    }
}
