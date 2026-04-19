package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.CustomerTokenCreateDTO;
import site.xlinks.ai.router.dto.CustomerTokenUpdateDTO;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.service.CustomerTokenService;

/**
 * Customer token management API.
 */
@RestController
@RequestMapping("/api/customer-tokens")
@RequiredArgsConstructor
@Tag(name = "Customer Token Management", description = "Customer token management APIs")
public class CustomerTokenController {

    private final CustomerTokenService customerTokenService;

    @PostMapping
    @Operation(summary = "Create customer token")
    public Result<CustomerToken> create(@Valid @RequestBody CustomerTokenCreateDTO dto) {
        CustomerToken token = new CustomerToken();
        token.setCustomerName(dto.getCustomerName());
        token.setTokenName(dto.getTokenName());
        token.setStatus(dto.getStatus());
        token.setExpireTime(dto.getExpireTime());
        token.setAllowedModels(dto.getAllowedModels());
        token.setDailyQuota(dto.getDailyQuota());
        token.setTotalQuota(dto.getTotalQuota());
        token.setRemark(dto.getRemark());

        CustomerToken created = customerTokenService.create(token);
        return Result.success(created);
    }

    @GetMapping
    @Operation(summary = "Customer token list")
    public Result<PageResult<CustomerToken>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer status) {

        var pageResult = customerTokenService.pageQuery(page, pageSize, customerName, status);
        pageResult.getRecords().forEach(t -> t.setTokenValue(null));
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Customer token detail")
    public Result<CustomerToken> get(@PathVariable Long id) {
        CustomerToken token = customerTokenService.getById(id);
        token.setTokenValue(null);
        return Result.success(token);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer token")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerTokenUpdateDTO dto) {
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
        if (dto.getDailyQuota() != null) {
            token.setDailyQuota(dto.getDailyQuota());
        }
        if (dto.getTotalQuota() != null) {
            token.setTotalQuota(dto.getTotalQuota());
        }
        if (dto.getRemark() != null) {
            token.setRemark(dto.getRemark());
        }

        customerTokenService.update(token);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable customer token")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        customerTokenService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer token")
    public Result<Void> delete(@PathVariable Long id) {
        customerTokenService.deleteById(id);
        return Result.success();
    }
}
