package site.xlinks.ai.router.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.dashboard.DashboardStatsResponse;
import site.xlinks.ai.router.client.dto.dashboard.ModelUsageItemResponse;
import site.xlinks.ai.router.client.dto.dashboard.RecentActivityResponse;
import site.xlinks.ai.router.client.dto.dashboard.UsageTrendItemResponse;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.mapper.UsageRecordMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
@RequestMapping("/api/v1/usages")
@RequiredArgsConstructor
public class CustomerTokenUsageController {

    private final UsageRecordMapper usageRecordMapper;
    private final CustomerTokenMapper customerTokenMapper;

    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getStats(@RequestParam String customerToken) {
        CustomerToken token = resolveCustomerToken(customerToken);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.minusDays(1).atTime(LocalTime.MAX);

        StatsSummary todaySummary = querySummary(token.getAccountId(), token.getTokenValue(), todayStart, todayEnd);
        StatsSummary yesterdaySummary = querySummary(token.getAccountId(), token.getTokenValue(), yesterdayStart, yesterdayEnd);

        DashboardStatsResponse response = new DashboardStatsResponse();
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
    public Result<List<UsageTrendItemResponse>> getUsageTrend(@RequestParam String customerToken,
                                                              @RequestParam(defaultValue = "7") Integer days) {
        CustomerToken token = resolveCustomerToken(customerToken);
        if (days == null || days <= 0) {
            return Result.success(List.of());
        }

        int hours = days * 24;
        LocalDateTime endTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startTime = endTime.minusHours(hours - 1L);

        List<UsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, token.getAccountId())
                        .eq(UsageRecord::getCustomerToken, token.getTokenValue())
                        .between(UsageRecord::getCreatedAt, startTime, endTime.plusHours(1))
        );

        Map<LocalDateTime, StatsSummary> bucketMap = new HashMap<>();
        for (UsageRecord record : records) {
            LocalDateTime createdAt = record.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            LocalDateTime bucket = createdAt.truncatedTo(ChronoUnit.HOURS);
            StatsSummary summary = bucketMap.computeIfAbsent(bucket, key -> new StatsSummary(0L, 0L, BigDecimal.ZERO));
            long tokenCount = record.getTotalTokens() == null ? 0L : record.getTotalTokens().longValue();
            BigDecimal cost = record.getTotalCost() == null ? BigDecimal.ZERO : record.getTotalCost();
            bucketMap.put(bucket, new StatsSummary(summary.requests + 1, summary.tokens + tokenCount, summary.cost.add(cost)));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:00");
        List<UsageTrendItemResponse> trend = new ArrayList<>(hours);
        for (int i = 0; i < hours; i++) {
            LocalDateTime bucketTime = startTime.plusHours(i);
            StatsSummary summary = bucketMap.getOrDefault(bucketTime, new StatsSummary(0L, 0L, BigDecimal.ZERO));
            trend.add(new UsageTrendItemResponse(bucketTime.format(formatter), summary.tokens, summary.cost));
        }
        return Result.success(trend);
    }

    @GetMapping("/model-usage")
    public Result<List<ModelUsageItemResponse>> getModelUsage(@RequestParam String customerToken) {
        CustomerToken token = resolveCustomerToken(customerToken);

        List<UsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, token.getAccountId())
                        .eq(UsageRecord::getCustomerToken, token.getTokenValue())
        );

        Map<String, StatsSummary> modelSummary = new HashMap<>();
        for (UsageRecord record : records) {
            String modelName = record.getModelName();
            if (modelName == null || modelName.isBlank()) {
                modelName = record.getModelCode() == null ? "Unknown Model" : record.getModelCode();
            }

            long tokenCount = record.getTotalTokens() == null ? 0L : record.getTotalTokens().longValue();
            if (tokenCount <= 0) {
                continue;
            }

            StatsSummary summary = modelSummary.getOrDefault(modelName, new StatsSummary(0L, 0L, BigDecimal.ZERO));
            BigDecimal cost = record.getTotalCost() == null ? BigDecimal.ZERO : record.getTotalCost();
            modelSummary.put(modelName, new StatsSummary(summary.requests + 1, summary.tokens + tokenCount, summary.cost.add(cost)));
        }

        List<ModelUsageItemResponse> responses = new ArrayList<>();
        for (Map.Entry<String, StatsSummary> entry : modelSummary.entrySet()) {
            StatsSummary summary = entry.getValue();
            responses.add(new ModelUsageItemResponse(entry.getKey(), summary.requests, summary.tokens, summary.cost));
        }
        responses.sort((left, right) -> Long.compare(right.getTokens(), left.getTokens()));
        return Result.success(responses);
    }

    @GetMapping("/recent-activities")
    public Result<PageResult<RecentActivityResponse>> getRecentActivities(@RequestParam String customerToken,
                                                                          @RequestParam(defaultValue = "1") Integer page,
                                                                          @RequestParam(defaultValue = "20") Integer pageSize,
                                                                          @RequestParam(required = false) Integer limit) {
        CustomerToken token = resolveCustomerToken(customerToken);

        if (limit != null && limit > 0) {
            page = 1;
            pageSize = limit;
        }

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 20) {
            pageSize = 20;
        }

        Long totalCount = usageRecordMapper.selectCount(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, token.getAccountId())
                        .eq(UsageRecord::getCustomerToken, token.getTokenValue())
        );
        long total = totalCount == null ? 0L : totalCount;
        long offset = (long) (page - 1) * pageSize;
        if (offset >= total) {
            return Result.success(PageResult.of(List.of(), total, page, pageSize));
        }

        List<UsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, token.getAccountId())
                        .eq(UsageRecord::getCustomerToken, token.getTokenValue())
                        .orderByDesc(UsageRecord::getCreatedAt)
                        .last("limit " + offset + "," + pageSize)
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<RecentActivityResponse> responses = new ArrayList<>();
        String maskedToken = maskToken(token.getTokenValue());
        for (UsageRecord record : records) {
            String time = record.getCreatedAt() == null ? "" : record.getCreatedAt().format(formatter);
            String channel = record.getEndpointCode() == null ? "" : record.getEndpointCode();
            String model = record.getModelName() == null || record.getModelName().isBlank()
                    ? (record.getModelCode() == null ? "" : record.getModelCode())
                    : record.getModelName();
            responses.add(new RecentActivityResponse(
                    time,
                    maskedToken,
                    channel,
                    model,
                    record.getPromptTokens(),
                    record.getCacheHitTokens(),
                    record.getCompletionTokens(),
                    record.getTotalTokens(),
                    record.getResponseMs(),
                    record.getUsageType(),
                    record.getTotalCost()
            ));
        }
        return Result.success(PageResult.of(responses, total, page, pageSize));
    }

    private CustomerToken resolveCustomerToken(String customerToken) {
        String normalized = customerToken == null ? "" : customerToken.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "customerToken不能为空");
        }

        CustomerToken token = customerTokenMapper.selectOne(
                new LambdaQueryWrapper<CustomerToken>()
                        .eq(CustomerToken::getTokenValue, normalized)
                        .last("limit 1")
        );
        if (token == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "customerToken不存在");
        }
        return token;
    }

    private StatsSummary querySummary(Long accountId, String customerToken, LocalDateTime start, LocalDateTime end) {
        List<UsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getAccountId, accountId)
                        .eq(UsageRecord::getCustomerToken, customerToken)
                        .between(UsageRecord::getCreatedAt, start, end)
        );
        long requests = records.size();
        long tokens = records.stream().mapToLong(record -> record.getTotalTokens() == null ? 0L : record.getTotalTokens().longValue()).sum();
        BigDecimal cost = records.stream()
                .map(record -> record.getTotalCost() == null ? BigDecimal.ZERO : record.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StatsSummary(requests, tokens, cost);
    }

    private Double calcChangePercent(long today, long yesterday) {
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
                .divide(yesterday, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.length() <= 8) {
            return token.charAt(0) + "****" + token.charAt(token.length() - 1);
        }
        return token.substring(0, 3) + "****" + token.substring(token.length() - 4);
    }

    private static class StatsSummary {
        private final long requests;
        private final long tokens;
        private final BigDecimal cost;

        private StatsSummary(long requests, long tokens, BigDecimal cost) {
            this.requests = requests;
            this.tokens = tokens;
            this.cost = cost;
        }
    }
}
