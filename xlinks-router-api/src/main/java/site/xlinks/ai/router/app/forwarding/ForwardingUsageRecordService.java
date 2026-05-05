package site.xlinks.ai.router.app.forwarding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.common.enums.ProviderCacheHitStrategy;
import site.xlinks.ai.router.app.forwarding.model.ForwardingUsageContext;
import site.xlinks.ai.router.app.forwarding.model.UsageMetrics;
import site.xlinks.ai.router.infrastructure.cache.RoutingSnapshotCacheService;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.UsageRecordMapper;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingUsageRecordService {

    private final UsageRecordMapper usageRecordMapper;
    private final CustomerPlanService customerPlanService;
    private final CustomerTokenQuotaService customerTokenQuotaService;
    private final ProviderTokenQuotaService providerTokenQuotaService;
    private final WalletService walletService;
    private final RoutingSnapshotCacheService routingSnapshotCacheService;

    @Async("usageTaskExecutor")
    public void recordSuccessAsync(ForwardingUsageContext context,
                                   UsageMetrics usageMetrics,
                                   long sessionMs) {
        record(context, usageMetrics, sessionMs, null, null, "success");
    }

    @Async("usageTaskExecutor")
    public void recordErrorAsync(ForwardingUsageContext context,
                                 String errorCode,
                                 String errorMessage,
                                 long sessionMs,
                                 String finishReason) {
        record(context, null, sessionMs, errorCode, errorMessage, finishReason);
    }

    public void record(ForwardingUsageContext context,
                       UsageMetrics usageMetrics,
                       long sessionMs,
                       String errorCode,
                       String errorMessage,
                       String finishReason) {
        if (context == null) {
            return;
        }
        UsageRecord record = buildRecord(context, usageMetrics);
        record.setResponseStatus(errorCode == null ? 200 : 500);
        int normalizedSessionMs = normalizeDurationMs(sessionMs);
        record.setSessionMs(normalizedSessionMs);
        record.setResponseMs(normalizedSessionMs);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        record.setFinishReason(finishReason);
        try {
            insertUsageRecordWithRetry(record, context.getRequestId());
            settleUsage(context, record);
            syncCustomerTokenQuotaUsage(context, record);
            syncProviderTokenUsage(context, record);
            log.debug("用量记录保存完成。requestId={}", context.getRequestId());
        } catch (Exception ex) {
            log.error("保存用量记录失败。requestId={}", context.getRequestId(), ex);
        }
    }

    private UsageRecord buildRecord(ForwardingUsageContext context, UsageMetrics usageMetrics) {
        UsageRecord record = new UsageRecord();
        record.setRequestId(context.getRequestId());
        record.setAccountId(context.getAccountId());
        record.setCustomerToken(context.getCustomerTokenValue());
        record.setProviderToken(context.getProviderTokenValue());
        record.setProviderTokenId(context.getProviderTokenId());
        record.setUsageType(context.getPlanId() == null ? "balance" : "plan");
        record.setUsageFrom(context.getPlanId() == null ? null : String.valueOf(context.getPlanId()));
        record.setProviderId(context.getProviderId());
        record.setProviderCode(context.getProviderCode());
        record.setProviderName(context.getProviderName());
        record.setEndpointCode(context.getEndpointCode());
        record.setModelId(context.getModelId());
        record.setModelCode(context.getModelCode());
        record.setModelName(context.getModelName());

        int promptTokens;
        int completionTokens;
        int totalTokens;
        int cacheHitTokens;
        if (usageMetrics == null) {
            promptTokens = 0;
            completionTokens = 0;
            totalTokens = 0;
            cacheHitTokens = 0;
        } else {
            promptTokens = defaultInt(usageMetrics.getInputTokens());
            completionTokens = defaultInt(usageMetrics.getOutputTokens());
            totalTokens = usageMetrics.getTotalTokens() == null
                    ? promptTokens + completionTokens
                    : defaultInt(usageMetrics.getTotalTokens());
            cacheHitTokens = normalizeCacheHitTokens(
                    usageMetrics.getCacheHitTokens(),
                    promptTokens,
                    context.getModelProvider()
            );
        }

        int promptBillableTokens = Math.max(promptTokens - cacheHitTokens, 0);
        record.setPromptTokens(promptTokens);
        record.setCompletionTokens(completionTokens);
        record.setTotalTokens(totalTokens);
        record.setCacheHitTokens(cacheHitTokens);

        BigDecimal promptCost = calculateCost(context.getInputPrice(), promptBillableTokens);
        BigDecimal cacheHitCost = applyMultiplier(
                calculateCost(resolveCacheHitPrice(context), cacheHitTokens),
                context.getMultiplier()
        );
        BigDecimal completionCost = calculateCost(context.getOutputPrice(), completionTokens);

        record.setPromptCost(promptCost);
        record.setCacheHitCost(cacheHitCost);
        record.setCompletionCost(completionCost);
        record.setTotalCost(promptCost.add(cacheHitCost).add(completionCost));
        return record;
    }

    private void settleUsage(ForwardingUsageContext context, UsageRecord record) {
        if (record.getTotalCost() == null || record.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (context.getPlanId() != null) {
            customerPlanService.consumeQuota(context.getPlanId(), record.getTotalCost());
            return;
        }
        if (context.getAccountId() == null) {
            return;
        }
        WalletService.BasicWalletDebitResult debitResult = walletService.debitBasicAllowOverdraftToZero(
                context.getAccountId(),
                record.getTotalCost(),
                WalletConstants.BIZ_TYPE_API_USAGE,
                context.getRequestId(),
                "接口调用结算：" + context.getModelCode()
        );
        routingSnapshotCacheService.refreshWalletByAccountId(context.getAccountId());
        if (debitResult != null && debitResult.overdraftApplied()) {
            log.warn("余额结算发生缺口。requestId={}, accountId={}, totalCost={}, debitedAmount={}, shortfallAmount={}",
                    context.getRequestId(),
                    context.getAccountId(),
                    record.getTotalCost(),
                    debitResult.debitedAmount(),
                    debitResult.shortfallAmount());
        }
    }

    private void syncCustomerTokenQuotaUsage(ForwardingUsageContext context, UsageRecord record) {
        if (context.getCustomerTokenId() == null
                || context.getAccountId() == null
                || context.getCustomerTokenValue() == null
                || context.getCustomerTokenValue().isBlank()) {
            return;
        }
        BigDecimal totalCost = record.getTotalCost();
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal todayUsed = usageRecordMapper.sumTotalCostByDateRange(
                context.getAccountId(),
                context.getCustomerTokenValue(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );
        customerTokenQuotaService.syncQuotaUsage(
                context.getCustomerTokenId(),
                todayUsed == null ? BigDecimal.ZERO : todayUsed,
                totalCost
        );
    }

    private void syncProviderTokenUsage(ForwardingUsageContext context, UsageRecord record) {
        providerTokenQuotaService.syncUsage(context.getProviderTokenId(), record.getTotalTokens());
    }

    private void insertUsageRecordWithRetry(UsageRecord record, String requestId) {
        try {
            usageRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            Long conflictedId = record.getId();
            log.warn("用量记录主键冲突，准备重试插入。requestId={}, conflictedId={}", requestId, conflictedId);
            record.setId(null);
            usageRecordMapper.insert(record);
        }
    }

    private int normalizeCacheHitTokens(Integer cacheHitTokens, int promptTokens, String modelProvider) {
        ProviderCacheHitStrategy strategy = ProviderCacheHitStrategy.fromModelProvider(modelProvider);
        if (!strategy.isCacheHitSupported() || promptTokens <= 0) {
            return 0;
        }
        int value = defaultInt(cacheHitTokens);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, promptTokens);
    }

    private BigDecimal resolveCacheHitPrice(ForwardingUsageContext context) {
        return context.getCacheHitPrice() == null ? context.getInputPrice() : context.getCacheHitPrice();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal calculateCost(BigDecimal pricePerMillion, int tokens) {
        if (pricePerMillion == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerMillion
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal applyMultiplier(BigDecimal cost, BigDecimal multiplier) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : multiplier;
        return cost.multiply(normalized).setScale(6, RoundingMode.HALF_UP);
    }

    private int normalizeDurationMs(long durationMs) {
        if (durationMs <= 0) {
            return 0;
        }
        if (durationMs > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) durationMs;
    }
}
