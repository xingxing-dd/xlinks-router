package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.enums.ProviderCacheHitStrategy;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
/**
 * Usage record service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private static final String BASIC_WALLET_BALANCE_INSUFFICIENT = "basic wallet balance is insufficient";

    private final site.xlinks.ai.router.mapper.UsageRecordMapper usageRecordMapper;
    private final CustomerPlanService customerPlanService;
    private final CustomerTokenQuotaService customerTokenQuotaService;
    private final WalletService walletService;
    private final UsageEntitlementService usageEntitlementService;

    public void record(ProviderInvokeContext context,
                       UsageMetrics usageMetrics,
                       long sessionMs,
                       String errorCode,
                       String errorMessage,
                       String finishReason) {
        record(context, usageMetrics, sessionMs, normalizeDurationMs(sessionMs), errorCode, errorMessage, finishReason);
    }

    public void record(ProviderInvokeContext context,
                       UsageMetrics usageMetrics,
                       long sessionMs,
                       Integer responseMs,
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
        // Non-streaming requests and stream fallbacks should still persist a usable first-response duration.
        record.setResponseMs(responseMs == null ? normalizedSessionMs : normalizeDurationMs(responseMs.longValue()));
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        record.setFinishReason(finishReason);
        try {
            insertUsageRecordWithRetry(record, context.getRequestId());
            consumeUsageBalanceOrPlan(context, record);
            syncCustomerTokenQuotaUsage(context, record);
            log.debug("Usage record saved: {}", context.getRequestId());
        } catch (Exception e) {
            log.error("Failed to save usage record", e);
        }
    }

    @Async("usageTaskExecutor")
    public void recordAsync(ProviderInvokeContext context,
                            UsageMetrics usageMetrics,
                            long sessionMs,
                            String errorCode,
                            String errorMessage,
                            String finishReason) {
        record(context, usageMetrics, sessionMs, normalizeDurationMs(sessionMs), errorCode, errorMessage, finishReason);
    }

    @Async("usageTaskExecutor")
    public void recordAsync(ProviderInvokeContext context,
                            UsageMetrics usageMetrics,
                            long sessionMs,
                            Integer responseMs,
                            String errorCode,
                            String errorMessage,
                            String finishReason) {
        record(context, usageMetrics, sessionMs, responseMs, errorCode, errorMessage, finishReason);
    }

    public void recordError(ProviderInvokeContext context,
                            int responseStatus,
                            String errorCode,
                            String errorMessage,
                            long sessionMs,
                            String finishReason) {
        if (context == null) {
            return;
        }
        UsageRecord record = buildRecord(context, null);
        fillErrorRecord(record, responseStatus, errorCode, errorMessage, sessionMs, finishReason);
        persistErrorRecord(record, context.getRequestId(), "Error usage record saved: {}");
    }

    public void recordRoutingError(RoutingBuildContext context,
                                   int responseStatus,
                                   String errorCode,
                                   String errorMessage,
                                   long sessionMs,
                                   String finishReason) {
        if (context == null || context.getCustomerToken() == null) {
            return;
        }
        UsageRecord record = buildRoutingErrorRecord(context);
        fillErrorRecord(record, responseStatus, errorCode, errorMessage, sessionMs, finishReason);
        persistErrorRecord(record, context.getRequestId(), "Routing error usage record saved: {}");
    }

    private void fillErrorRecord(UsageRecord record,
                                 int responseStatus,
                                 String errorCode,
                                 String errorMessage,
                                 long sessionMs,
                                 String finishReason) {
        record.setResponseStatus(responseStatus);
        record.setSessionMs(normalizeDurationMs(sessionMs));
        record.setResponseMs(null);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        record.setFinishReason(finishReason);
    }

    private void persistErrorRecord(UsageRecord record, String requestId, String successLogTemplate) {
        try {
            insertUsageRecordWithRetry(record, requestId);
            log.debug(successLogTemplate, requestId);
        } catch (Exception e) {
            log.error("Failed to save error usage record", e);
        }
    }

    private void insertUsageRecordWithRetry(UsageRecord record, String requestId) {
        try {
            usageRecordMapper.insert(record);
        } catch (DuplicateKeyException duplicateKeyException) {
            Long conflictedId = record.getId();
            log.warn("Duplicate usage record id detected, retrying insert with a new id, requestId={}, conflictedId={}",
                    requestId, conflictedId);
            record.setId(null);
            usageRecordMapper.insert(record);
        }
    }

    private UsageRecord buildRecord(ProviderInvokeContext context, UsageMetrics usageMetrics) {
        UsageRecord record = new UsageRecord();
        record.setRequestId(context.getRequestId());
        record.setAccountId(context.getAccountId());
        record.setCustomerToken(context.getCustomerToken());
        record.setProviderToken(context.getProviderToken());
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
        if (usageMetrics != null) {
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
        } else {
            promptTokens = 0;
            completionTokens = 0;
            totalTokens = 0;
            cacheHitTokens = 0;
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

    private UsageRecord buildRoutingErrorRecord(RoutingBuildContext context) {
        UsageRecord record = new UsageRecord();
        CustomerToken customerToken = context.getCustomerToken();
        UsageDecision usageDecision = context.getUsageDecision();
        Model model = context.getModel();

        record.setRequestId(context.getRequestId());
        record.setAccountId(customerToken.getAccountId());
        record.setCustomerToken(context.getToken());
        record.setProviderToken(null);
        record.setProviderTokenId(null);
        record.setUsageType(resolveUsageType(usageDecision));
        record.setUsageFrom(usageDecision != null && usageDecision.getPlanId() != null
                ? String.valueOf(usageDecision.getPlanId())
                : null);
        record.setProviderId(null);
        record.setProviderCode(null);
        record.setProviderName(null);
        record.setEndpointCode(context.getRequest() == null || context.getRequest().getProtocol() == null
                ? null
                : context.getRequest().getProtocol().getCode());
        record.setModelId(model == null ? null : model.getId());
        record.setModelCode(model == null ? null : model.getModelCode());
        record.setModelName(model == null ? null : model.getModelName());
        record.setPromptTokens(0);
        record.setCompletionTokens(0);
        record.setTotalTokens(0);
        record.setCacheHitTokens(0);
        record.setPromptCost(BigDecimal.ZERO);
        record.setCacheHitCost(BigDecimal.ZERO);
        record.setCompletionCost(BigDecimal.ZERO);
        record.setTotalCost(BigDecimal.ZERO);
        return record;
    }

    private String resolveUsageType(UsageDecision usageDecision) {
        if (usageDecision == null) {
            return null;
        }
        if (usageDecision.getPlanId() != null) {
            return "plan";
        }
        if (usageDecision.isBalanceEnabled()) {
            return "balance";
        }
        return null;
    }

    private void consumeUsageBalanceOrPlan(ProviderInvokeContext context, UsageRecord record) {
        if (context == null || record == null) {
            return;
        }
        if (record.getTotalCost() == null || record.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Long planId = context.getPlanId();
        if (planId != null) {
            customerPlanService.consumeQuota(planId, record.getTotalCost());
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
                "API usage completed: " + context.getModelCode()
        );
        if (debitResult != null && debitResult.walletBundle() != null && debitResult.walletBundle().getMainWallet() != null) {
            usageEntitlementService.syncBalanceAvailability(
                    context.getAccountId(),
                    debitResult.walletBundle().getMainWallet().getAvailableBalance()
            );
        }
        if (debitResult != null && debitResult.overdraftApplied()) {
            log.warn("请求计费时基础钱包余额不足，已按允许的小额超额使用处理并将余额归零，requestId={}, accountId={}, modelCode={}, totalCost={}, debitedAmount={}, shortfallAmount={}",
                    context.getRequestId(),
                    context.getAccountId(),
                    context.getModelCode(),
                    record.getTotalCost(),
                    debitResult.debitedAmount(),
                    debitResult.shortfallAmount());
        }
    }

    private void consumeUsageBalanceOrPlanSafely(ProviderInvokeContext context, UsageRecord record) {
        try {
            consumeUsageBalanceOrPlan(context, record);
        } catch (BusinessException e) {
            if (isAllowedBasicWalletOverdraft(e)) {
                log.warn("请求计费时基础钱包余额不足，已按允许的小额透支处理，requestId={}, accountId={}, modelCode={}, totalCost={}",
                        context == null ? null : context.getRequestId(),
                        context == null ? null : context.getAccountId(),
                        context == null ? null : context.getModelCode(),
                        record == null ? null : record.getTotalCost());
                return;
            }
            throw e;
        }
    }

    private void syncCustomerTokenQuotaUsage(ProviderInvokeContext context, UsageRecord record) {
        if (context == null || record == null || context.getCustomerTokenId() == null) {
            return;
        }
        if (context.getAccountId() == null || context.getCustomerToken() == null || context.getCustomerToken().isBlank()) {
            return;
        }
        BigDecimal totalCost = record.getTotalCost();
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal todayUsed = usageRecordMapper.sumTotalCostByDateRange(
                context.getAccountId(),
                context.getCustomerToken(),
                java.time.LocalDate.now().atStartOfDay(),
                java.time.LocalDate.now().plusDays(1).atStartOfDay()
        );
        if (todayUsed == null) {
            todayUsed = BigDecimal.ZERO;
        }
        customerTokenQuotaService.syncQuotaUsage(context.getCustomerTokenId(), todayUsed, totalCost);
    }

    private boolean isAllowedBasicWalletOverdraft(BusinessException exception) {
        if (exception == null) {
            return false;
        }
        return exception.getCode() == ErrorCode.FORBIDDEN.getCode()
                && BASIC_WALLET_BALANCE_INSUFFICIENT.equals(exception.getMessage());
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

    private BigDecimal resolveCacheHitPrice(ProviderInvokeContext context) {
        if (context == null) {
            return null;
        }
        if (context.getCacheHitPrice() != null) {
            return context.getCacheHitPrice();
        }
        // Backward compatibility for old model rows that have no cache_hit_price yet.
        return context.getInputPrice();
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
