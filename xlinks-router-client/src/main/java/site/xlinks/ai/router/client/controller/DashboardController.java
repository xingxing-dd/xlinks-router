package site.xlinks.ai.router.client.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.dashboard.DashboardStatsResponse;
import site.xlinks.ai.router.client.dto.dashboard.ModelUsageItemResponse;
import site.xlinks.ai.router.client.dto.dashboard.RecentActivityResponse;
import site.xlinks.ai.router.client.dto.dashboard.UsageTrendItemResponse;
import site.xlinks.ai.router.common.result.Result;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getStats() {
        DashboardStatsResponse response = new DashboardStatsResponse();
        response.setTodayRequests(3245);
        response.setTodayRequestsChange(12.5D);
        response.setTodayTokens(28500);
        response.setTodayTokensChange(8.2D);
        response.setTodayCost(new BigDecimal("56.80"));
        response.setTodayCostChange(-3.1D);
        response.setBalance(new BigDecimal("1258.00"));
        return Result.success(response);
    }

    @GetMapping("/usage-trend")
    public Result<List<UsageTrendItemResponse>> getUsageTrend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(List.of(
                new UsageTrendItemResponse("03-03", 12000, new BigDecimal("240.00")),
                new UsageTrendItemResponse("03-04", 15000, new BigDecimal("300.00")),
                new UsageTrendItemResponse("03-05", 18000, new BigDecimal("360.00")),
                new UsageTrendItemResponse("03-06", 14000, new BigDecimal("280.00")),
                new UsageTrendItemResponse("03-07", 22000, new BigDecimal("440.00")),
                new UsageTrendItemResponse("03-08", 25000, new BigDecimal("500.00")),
                new UsageTrendItemResponse("03-09", 28000, new BigDecimal("560.00"))
        ));
    }

    @GetMapping("/model-usage")
    public Result<List<ModelUsageItemResponse>> getModelUsage() {
        return Result.success(List.of(
                new ModelUsageItemResponse("GPT-4", 850, 125000, new BigDecimal("1250.00")),
                new ModelUsageItemResponse("GPT-3.5", 1200, 180000, new BigDecimal("360.00")),
                new ModelUsageItemResponse("Claude-3", 650, 95000, new BigDecimal("1425.00")),
                new ModelUsageItemResponse("Gemini", 420, 63000, new BigDecimal("126.00"))
        ));
    }

    @GetMapping("/recent-activities")
    public Result<List<RecentActivityResponse>> getRecentActivities(@RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(List.of(
                new RecentActivityResponse("2 分钟前", "GPT-4 API 调用成功", "1,245 tokens", "success"),
                new RecentActivityResponse("15 分钟前", "Claude-3 API 调用成功", "856 tokens", "success"),
                new RecentActivityResponse("1 小时前", "账户充值 ¥500", "", "info"),
                new RecentActivityResponse("2 小时前", "GPT-3.5 API 调用失败", "重试中", "error"),
                new RecentActivityResponse("3 小时前", "新增 API Key", "sk-***abc", "info")
        ));
    }
}
