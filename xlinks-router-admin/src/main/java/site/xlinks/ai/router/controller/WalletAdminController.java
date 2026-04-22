package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.WalletAmountOperationDTO;
import site.xlinks.ai.router.dto.WalletOrderAuditDTO;
import site.xlinks.ai.router.dto.WalletStateUpdateDTO;
import site.xlinks.ai.router.service.WalletAdminService;
import site.xlinks.ai.router.vo.WalletDetailVO;
import site.xlinks.ai.router.vo.WalletListVO;
import site.xlinks.ai.router.vo.WalletBatchOpenResultVO;
import site.xlinks.ai.router.vo.WalletMainFlowVO;
import site.xlinks.ai.router.vo.WalletSubFlowVO;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Wallet and ledger management APIs")
public class WalletAdminController {

    private final WalletAdminService walletAdminService;

    @GetMapping
    @Operation(summary = "Wallet list")
    public Result<PageResult<WalletListVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer walletStatus) {
        var pageResult = walletAdminService.pageQuery(page, pageSize, keyword, walletStatus);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }

    @PostMapping("/batch-open")
    @Operation(summary = "Batch open wallets for accounts without wallet")
    public Result<WalletBatchOpenResultVO> batchOpen() {
        return Result.success(walletAdminService.batchOpenMissingWallets());
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Wallet detail")
    public Result<WalletDetailVO> get(@PathVariable Long accountId) {
        return Result.success(walletAdminService.getDetail(accountId));
    }

    @GetMapping("/{accountId}/main-flows")
    @Operation(summary = "Main wallet flows")
    public Result<PageResult<WalletMainFlowVO>> mainFlows(@PathVariable Long accountId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        var pageResult = walletAdminService.pageMainFlows(accountId, page, pageSize);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }

    @GetMapping("/{accountId}/sub-flows")
    @Operation(summary = "Sub wallet flows")
    public Result<PageResult<WalletSubFlowVO>> subFlows(@PathVariable Long accountId,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "20") Integer pageSize,
                                                        @RequestParam(required = false) String walletType) {
        var pageResult = walletAdminService.pageSubFlows(accountId, page, pageSize, walletType);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }

    @PostMapping("/{accountId}/manual-credit")
    @Operation(summary = "Manual credit basic wallet")
    public Result<WalletDetailVO> manualCredit(@PathVariable Long accountId, @Valid @RequestBody WalletAmountOperationDTO dto) {
        return Result.success(walletAdminService.manualCredit(accountId, dto.getAmount(), dto.getOrderNo(), dto.getRemark()));
    }

    @PostMapping("/{accountId}/manual-debit")
    @Operation(summary = "Manual debit basic wallet")
    public Result<WalletDetailVO> manualDebit(@PathVariable Long accountId, @Valid @RequestBody WalletAmountOperationDTO dto) {
        return Result.success(walletAdminService.manualDebit(accountId, dto.getAmount(), dto.getOrderNo(), dto.getRemark()));
    }

    @PostMapping("/{accountId}/freeze")
    @Operation(summary = "Freeze basic wallet amount")
    public Result<WalletDetailVO> freeze(@PathVariable Long accountId, @Valid @RequestBody WalletAmountOperationDTO dto) {
        return Result.success(walletAdminService.freeze(accountId, dto.getAmount(), dto.getOrderNo(), dto.getRemark()));
    }

    @PostMapping("/{accountId}/unfreeze")
    @Operation(summary = "Unfreeze amount back to basic wallet")
    public Result<WalletDetailVO> unfreeze(@PathVariable Long accountId, @Valid @RequestBody WalletAmountOperationDTO dto) {
        return Result.success(walletAdminService.unfreeze(accountId, dto.getAmount(), dto.getOrderNo(), dto.getRemark()));
    }

    @PostMapping("/{accountId}/pending-settlement")
    @Operation(summary = "Credit pending settlement wallet")
    public Result<WalletDetailVO> pendingSettlement(@PathVariable Long accountId, @Valid @RequestBody WalletAmountOperationDTO dto) {
        return Result.success(walletAdminService.creditPendingSettlement(accountId, dto.getAmount(), dto.getOrderNo(), dto.getRemark()));
    }

    @PostMapping("/{accountId}/settle")
    @Operation(summary = "Settle pending settlement to basic wallet")
    public Result<WalletDetailVO> settle(@PathVariable Long accountId, @Valid @RequestBody WalletAmountOperationDTO dto) {
        return Result.success(walletAdminService.settlePending(accountId, dto.getAmount(), dto.getOrderNo(), dto.getRemark()));
    }

    @PatchMapping("/{accountId}/state")
    @Operation(summary = "Update wallet state")
    public Result<WalletDetailVO> updateState(@PathVariable Long accountId, @RequestBody WalletStateUpdateDTO dto) {
        return Result.success(walletAdminService.updateState(accountId, dto.getAllowIn(), dto.getAllowOut(), dto.getStatus(), dto.getRemark()));
    }

    @PostMapping("/withdraw-orders/{orderNo}/approve")
    @Operation(summary = "Approve withdraw order")
    public Result<Void> approveWithdraw(@PathVariable String orderNo, @RequestBody(required = false) WalletOrderAuditDTO dto) {
        walletAdminService.approveWithdraw(orderNo, dto == null ? null : dto.getRemark());
        return Result.success();
    }

    @PostMapping("/withdraw-orders/{orderNo}/reject")
    @Operation(summary = "Reject withdraw order")
    public Result<Void> rejectWithdraw(@PathVariable String orderNo, @RequestBody(required = false) WalletOrderAuditDTO dto) {
        walletAdminService.rejectWithdraw(orderNo, dto == null ? null : dto.getRemark());
        return Result.success();
    }
}
