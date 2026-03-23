package site.xlinks.ai.router.client.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.dashboard.DashboardStatsResponse;
import site.xlinks.ai.router.client.dto.dashboard.ModelUsageItemResponse;
import site.xlinks.ai.router.client.dto.dashboard.RecentActivityResponse;
import site.xlinks.ai.router.client.dto.dashboard.UsageTrendItemResponse;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.UsageRecordMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UsageRecordMapper usageRecordMapper;

    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getStats() {
        DashboardStatsResponse response = new DashboardStatsResponse();
        Long accountId = CustomerAccountContext.getAccountId();
        if (accountId == null) {
            response.setTodayRequests(0);
            response.setTodayRequestsChange(0D);
            response.setTodayTokens(0);
            response.setTodayTokensChange(0D);
            response.setTodayCost(BigDecimal.ZERO);
            response.setTodayCostChange(0D);
            response.setBalance(BigDecimal.ZERO);
            return Result.success(response);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.minusDays(1).atTime(LocalTime.MAX);

        StatsSummary todaySummary = querySummary(accountId, todayStart, todayEnd);
        StatsSummary yesterdaySummary = querySummary(accountId, yesterdayStart, yesterdayEnd);

        response.setTodayRequests(todaySummary.requests);
        response.setTodayRequestsChange(calcChangePercent(todaySummary.requests, yesterdaySummary.requests));
        response.setTodayTokens(todaySummary.tokens);
        response.setTodayTokensChange(calcChangePercent(todaySummary.tokens, yesterdaySummary.tokens));
        response.setTodayCost(todaySummary.cost);
        response.setTodayCostChange(calcChangePercent(todaySummary.cost, yesterdaySummary.cost));
        response.setBalance(BigDecimal.ZERO);
        return Result.success(response);
    }

    @GetMapping("/usage-trend")
    public Result<List<UsageTrendItemResponse>> getUsageTrend(@RequestParam(defaultValue = "7") Integer days) {
        Long accountId = CustomerAccountContext.getAccountId();
        if (accountId == null || days == null || days <= 0) {
            return Result.success(List.of());
        }
        int hours = days * 24;
        LocalDateTime endTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startTime = endTime.minusHours(hours - 1L);

        List<UsageRecord> records = usageRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, accountId)
                        .between(UsageRecord::getCreatedAt, startTime, endTime.plusHours(1))
        );

        Map<LocalDateTime, StatsSummary> bucketMap = new HashMap<>();
        for (UsageRecord record : records) {
            LocalDateTime createdAt = record.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            LocalDateTime bucket = createdAt.truncatedTo(ChronoUnit.HOURS);
            StatsSummary summary = bucketMap.computeIfAbsent(bucket, k -> new StatsSummary(0, 0, BigDecimal.ZERO));
            int tokenCount = record.getTotalTokens() == null ? 0 : record.getTotalTokens();
            BigDecimal cost = record.getTotalCost() == null ? BigDecimal.ZERO : record.getTotalCost();
            bucketMap.put(bucket, new StatsSummary(summary.requests + 1, summary.tokens + tokenCount, summary.cost.add(cost)));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:00");
        List<UsageTrendItemResponse> trend = new ArrayList<>(hours);
        for (int i = 0; i < hours; i++) {
            LocalDateTime bucketTime = startTime.plusHours(i);
            StatsSummary summary = bucketMap.getOrDefault(bucketTime, new StatsSummary(0, 0, BigDecimal.ZERO));
            trend.add(new UsageTrendItemResponse(bucketTime.format(formatter), summary.tokens, summary.cost));
        }
        return Result.success(trend);
    }

    @GetMapping("/model-usage")
    public Result<List<ModelUsageItemResponse>> getModelUsage() {
        Long accountId = CustomerAccountContext.getAccountId();
        if (accountId == null) {
            return Result.success(List.of());
        }
        List<UsageRecord> records = usageRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, accountId)
        );
        Map<String, StatsSummary> modelSummary = new HashMap<>();
        for (UsageRecord record : records) {
            String modelName = record.getModelName();
            if (modelName == null || modelName.isBlank()) {
                modelName = record.getModelCode() == null ? "未知模型" : record.getModelCode();
            }
            StatsSummary summary = modelSummary.getOrDefault(modelName, new StatsSummary(0, 0, BigDecimal.ZERO));
            int tokenCount = record.getTotalTokens() == null ? 0 : record.getTotalTokens();
            if (tokenCount <= 0) {
                continue;
            }
            BigDecimal cost = record.getTotalCost() == null ? BigDecimal.ZERO : record.getTotalCost();
            modelSummary.put(modelName, new StatsSummary(summary.requests + tokenCount, summary.tokens + tokenCount, summary.cost.add(cost)));
        }
        List<ModelUsageItemResponse> responses = new ArrayList<>();
        for (Map.Entry<String, StatsSummary> entry : modelSummary.entrySet()) {
            StatsSummary summary = entry.getValue();
            responses.add(new ModelUsageItemResponse(entry.getKey(), summary.requests, summary.tokens, summary.cost));
        }
        responses.sort((a, b) -> Integer.compare(b.getTokens(), a.getTokens()));
        return Result.success(responses);
    }

    @GetMapping("/recent-activities")
    public Result<List<RecentActivityResponse>> getRecentActivities(@RequestParam(defaultValue = "5") Integer limit) {
        Long accountId = CustomerAccountContext.getAccountId();
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        List<UsageRecord> records = usageRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, accountId)
                        .orderByDesc(UsageRecord::getCreatedAt)
                        .last("limit " + limit)
        );
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<RecentActivityResponse> responses = new ArrayList<>();
        for (UsageRecord record : records) {
            String time = record.getCreatedAt() == null ? "" : record.getCreatedAt().format(formatter);
            String tokenValue = record.getCustomerToken();
            String token = "";
            if (tokenValue != null && !tokenValue.isBlank()) {
                if (tokenValue.length() <= 8) {
                    token = tokenValue.charAt(0) + "****" + tokenValue.charAt(tokenValue.length() - 1);
                } else {
                    token = tokenValue.substring(0, 3) + "****" + tokenValue.substring(tokenValue.length() - 4);
                }
            }
            String channel = record.getEndpointCode() == null ? "" : record.getEndpointCode();
            String model = record.getModelName() == null || record.getModelName().isBlank()
                    ? (record.getModelCode() == null ? "" : record.getModelCode())
                    : record.getModelName();
            Integer inputTokens = record.getPromptTokens();
            Integer outputTokens = record.getCompletionTokens();
            Integer totalTokens = record.getTotalTokens();
            java.math.BigDecimal cost = record.getTotalCost();
            responses.add(new RecentActivityResponse(time, token, channel, model, inputTokens, outputTokens, totalTokens, cost));
        }
        return Result.success(responses);
    }

    private StatsSummary querySummary(Long accountId, LocalDateTime start, LocalDateTime end) {
        List<UsageRecord> records = usageRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, accountId)
                        .between(UsageRecord::getCreatedAt, start, end)
        );
        int requests = records.size();
        int tokens = records.stream().mapToInt(r -> r.getTotalTokens() == null ? 0 : r.getTotalTokens()).sum();
        BigDecimal cost = records.stream()
                .map(r -> r.getTotalCost() == null ? BigDecimal.ZERO : r.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StatsSummary(requests, tokens, cost);
    }

    private Double calcChangePercent(int today, int yesterday) {
        if (yesterday == 0) {
            return today == 0 ? 0D : 100D;
        }
        return (today - yesterday) * 100D / yesterday;
    }

    private Double calcChangePercent(BigDecimal today, BigDecimal yesterday) {
        if (yesterday == null || BigDecimal.ZERO.compareTo(yesterday) == 0) {
            return (today == null || BigDecimal.ZERO.compareTo(today) == 0) ? 0D : 100D;
        }
        if (today == null) {
            return -100D;
        }
        return today.subtract(yesterday)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterday, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static class StatsSummary {
        private final int requests;
        private final int tokens;
        private final BigDecimal cost;

        private StatsSummary(int requests, int tokens, BigDecimal cost) {
            this.requests = requests;
            this.tokens = tokens;
            this.cost = cost;
        }
    }
}
