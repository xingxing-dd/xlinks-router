package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.CustomerTokenCreateDTO;
import site.xlinks.ai.router.dto.CustomerTokenUpdateDTO;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.service.CustomerTokenService;

/**
 * Customer Token 管理接口
 */
@RestController
@RequestMapping("/admin/customer-tokens")
@RequiredArgsConstructor
@Tag(name = "Customer Token 管理", description = "Customer Token 管理相关接口")
public class CustomerTokenController {

    private final CustomerTokenService customerTokenService;

    @PostMapping
    @Operation(summary = "新增 Customer Token")
    public Result<CustomerToken> create(@RequestBody CustomerTokenCreateDTO dto) {
        CustomerToken token = new CustomerToken();
        token.setCustomerName(dto.getCustomerName());
        token.setTokenName(dto.getTokenName());
        token.setStatus(dto.getStatus());
        token.setExpireTime(dto.getExpireTime());
        token.setAllowedModels(dto.getAllowedModels());
        token.setRemark(dto.getRemark());
        
        // 创建 Token 并返回原始值（只返回一次）
        CustomerToken created = customerTokenService.create(token);
        return Result.success(created);
    }

    @GetMapping
    @Operation(summary = "Customer Token 列表")
    public Result<PageResult<CustomerToken>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer status) {
        
        var pageResult = customerTokenService.pageQuery(page, pageSize, customerName, status);
        // 不返回 tokenValue
        pageResult.getRecords().forEach(t -> t.setTokenValue(null));
        return Result.success(PageResult.of(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                (int) pageResult.getCurrent(), 
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Customer Token 详情")
    public Result<CustomerToken> get(@PathVariable Long id) {
        CustomerToken token = customerTokenService.getById(id);
        token.setTokenValue(null); // 不返回 tokenValue
        return Result.success(token);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 Customer Token")
    public Result<Void> update(@PathVariable Long id, @RequestBody CustomerTokenUpdateDTO dto) {
        CustomerToken token = new CustomerToken();
        token.setId(id);
        if (dto.getCustomerName() != null) {
            token.setCustomerName(dto.getCustomerName());
        }
        if (dto.getTokenName() != null) {
            token.setTokenName(dto.getTokenName());
        }
        if (dto.getExpireTime() != null) {
            token.setExpireTime(dto.getExpireTime());
        }
        if (dto.getAllowedModels() != null) {
            token.setAllowedModels(dto.getAllowedModels());
        }
        if (dto.getRemark() != null) {
            token.setRemark(dto.getRemark());
        }
        
        customerTokenService.update(token);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用 Customer Token")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        customerTokenService.updateStatus(id, status);
        return Result.success();
    }
}
