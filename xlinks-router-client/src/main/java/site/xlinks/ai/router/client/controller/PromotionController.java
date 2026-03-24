package site.xlinks.ai.router.client.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.promotion.PromotionInfoResponse;
import site.xlinks.ai.router.client.dto.promotion.PromotionRecordItemResponse;
import site.xlinks.ai.router.client.dto.promotion.PromotionRuleResponse;
import site.xlinks.ai.router.client.service.PromotionService;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;

@RestController
@RequestMapping("/api/v1/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/info")
    public Result<PromotionInfoResponse> getPromotionInfo() {
        return Result.success(promotionService.getPromotionInfo(getCurrentAccountId()));
    }

    @GetMapping("/records")
    public Result<PageResult<PromotionRecordItemResponse>> getPromotionRecords(@RequestParam(defaultValue = "1") Integer page,
                                                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        var pageResult = promotionService.pageRecords(getCurrentAccountId(), page, pageSize);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, pageSize));
    }

    @GetMapping("/rules")
    public Result<PromotionRuleResponse> getPromotionRules() {
        return Result.success(promotionService.getPromotionRules());
    }

    @GetMapping("/invite-code/check")
    public Result<Boolean> validateInviteCode(@RequestParam String inviteCode) {
        return Result.success(promotionService.validateInviteCode(inviteCode));
    }

    private Long getCurrentAccountId() {
        return CustomerAccountContext.requireAccount().getId();
    }
}
