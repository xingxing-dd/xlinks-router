package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.plan.CreateOrderResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletMainFlowResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletRechargeOrderRequest;
import site.xlinks.ai.router.client.dto.wallet.WalletSubFlowResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletSummaryResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletWithdrawOrderRequest;
import site.xlinks.ai.router.client.dto.wallet.WalletWithdrawOrderResponse;
import site.xlinks.ai.router.client.service.CustomerWalletFacadeService;
import site.xlinks.ai.router.client.service.WalletOrderService;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final CustomerWalletFacadeService customerWalletFacadeService;
    private final WalletOrderService walletOrderService;

    @GetMapping("/summary")
    public Result<WalletSummaryResponse> getSummary() {
        return Result.success(customerWalletFacadeService.getSummary(CustomerAccountContext.requireAccount().getId()));
    }

    @GetMapping("/main-flows")
    public Result<PageResult<WalletMainFlowResponse>> getMainFlows(@RequestParam(defaultValue = "1") Integer page,
                                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        var result = customerWalletFacadeService.pageMainFlows(CustomerAccountContext.requireAccount().getId(), page, pageSize);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize()));
    }

    @GetMapping("/sub-flows")
    public Result<PageResult<WalletSubFlowResponse>> getSubFlows(@RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "20") Integer pageSize,
                                                                 @RequestParam(required = false) String walletType) {
        var result = customerWalletFacadeService.pageSubFlows(CustomerAccountContext.requireAccount().getId(), page, pageSize, walletType);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize()));
    }

    @PostMapping("/recharge-orders")
    public Result<CreateOrderResponse> createRechargeOrder(@Valid @RequestBody WalletRechargeOrderRequest request) {
        return Result.success(walletOrderService.createRechargeOrder(
                CustomerAccountContext.requireAccount().getId(),
                request.getAmount(),
                request.getPaymentMethod()
        ));
    }

    @PostMapping("/withdraw-orders")
    public Result<WalletWithdrawOrderResponse> createWithdrawOrder(@Valid @RequestBody WalletWithdrawOrderRequest request) {
        return Result.success(customerWalletFacadeService.createWithdrawOrder(
                CustomerAccountContext.requireAccount().getId(),
                request.getAmount(),
                request.getRemark()
        ));
    }

    @PostMapping("/withdraw-orders/{orderNo}/cancel")
    public Result<Void> cancelWithdrawOrder(@PathVariable String orderNo) {
        customerWalletFacadeService.cancelWithdrawOrder(CustomerAccountContext.requireAccount().getId(), orderNo);
        return Result.success();
    }
}
