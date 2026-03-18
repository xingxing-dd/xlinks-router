package site.xlinks.ai.router.client.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.usage.UsageModelStatResponse;
import site.xlinks.ai.router.client.dto.usage.UsageProviderStatResponse;
import site.xlinks.ai.router.client.dto.usage.UsageRecordItemResponse;
import site.xlinks.ai.router.client.dto.usage.UsageSummaryResponse;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usage-records")
public class UsageRecordController {

    @GetMapping
    public Result<PageResult<UsageRecordItemResponse>> getUsageRecords(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        List<UsageRecordItemResponse> records = List.of(
                new UsageRecordItemResponse(1L, "req_abc123", "claude-sonnet", "OpenAI", 500, 745, 1245, 1500, 200, null, new BigDecimal("0.125"), "2026-03-17 14:00:00"),
                new UsageRecordItemResponse(2L, "req_def456", "claude-haiku", "Anthropic", 300, 420, 720, 980, 200, null, new BigDecimal("0.052"), "2026-03-17 12:00:00")
        );
        return Result.success(PageResult.of(records, records.size(), page, pageSize));
    }

    @GetMapping("/summary")
    public Result<UsageSummaryResponse> getUsageSummary() {
        UsageSummaryResponse response = new UsageSummaryResponse();
        response.setTotalRequests(10000);
        response.setTotalTokens(1500000);
        response.setTotalCost(new BigDecimal("1500.00"));
        response.setAvgLatencyMs(1200);
        response.setSuccessRate(99.5D);
        response.setModelStats(List.of(
                new UsageModelStatResponse("GPT-4", 5000, 800000, new BigDecimal("800.00")),
                new UsageModelStatResponse("Claude-3", 3000, 450000, new BigDecimal("550.00"))
        ));
        response.setProviderStats(List.of(
                new UsageProviderStatResponse("OpenAI", 6000, 900000, new BigDecimal("900.00")),
                new UsageProviderStatResponse("Anthropic", 4000, 600000, new BigDecimal("600.00"))
        ));
        return Result.success(response);
    }
}
