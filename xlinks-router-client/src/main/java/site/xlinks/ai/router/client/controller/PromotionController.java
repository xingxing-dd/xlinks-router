package site.xlinks.ai.router.client.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.promotion.PromotionInfoResponse;
import site.xlinks.ai.router.client.dto.promotion.PromotionRecordItemResponse;
import site.xlinks.ai.router.client.dto.promotion.PromotionRuleResponse;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotion")
public class PromotionController {

    @GetMapping("/info")
    public Result<PromotionInfoResponse> getPromotionInfo() {
        PromotionInfoResponse response = new PromotionInfoResponse();
        response.setReferralCode("INVITE2026ABC");
        response.setReferralLink("https://token-hub.com/register?ref=INVITE2026ABC");
        response.setTotalReferrals(23);
        response.setActiveReferrals(18);
        response.setTotalEarnings(new BigDecimal("1580.00"));
        response.setPendingEarnings(new BigDecimal("320.00"));
        return Result.success(response);
    }

    @GetMapping("/records")
    public Result<PageResult<PromotionRecordItemResponse>> getPromotionRecords(@RequestParam(defaultValue = "1") Integer page,
                                                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        List<PromotionRecordItemResponse> records = List.of(
                new PromotionRecordItemResponse("1", "张三", "zhang***@example.com", "2026-03-01", "active", new BigDecimal("150.00")),
                new PromotionRecordItemResponse("2", "李四", "li***@example.com", "2026-03-05", "active", new BigDecimal("200.00")),
                new PromotionRecordItemResponse("3", "王五", "wang***@example.com", "2026-03-08", "pending", new BigDecimal("50.00"))
        );
        return Result.success(PageResult.of(records, records.size(), page, pageSize));
    }

    @GetMapping("/rules")
    public Result<PromotionRuleResponse> getPromotionRules() {
        PromotionRuleResponse response = new PromotionRuleResponse();
        response.setRegisterReward(new BigDecimal("10.00"));
        response.setFirstRechargeRate(new BigDecimal("10"));
        response.setConsumptionRate(new BigDecimal("5"));
        response.setSettlementDay(1);
        return Result.success(response);
    }
}
