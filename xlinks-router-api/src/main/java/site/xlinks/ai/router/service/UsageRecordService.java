package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.entity.UsageRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Usage record service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private final site.xlinks.ai.router.mapper.UsageRecordMapper usageRecordMapper;
    private final CustomerPlanService customerPlanService;

    public void record(ProviderInvokeContext context,
                       UsageMetrics usageMetrics,
                       long latencyMs,
                       String errorCode,
                       String errorMessage) {
        if (context == null) {
            return;
        }
        UsageRecord record = buildRecord(context, usageMetrics);
        record.setResponseStatus(errorCode == null ? 200 : 500);
        record.setLatencyMs((int) latencyMs);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        try {
            usageRecordMapper.insert(record);
            consumePlanQuota(context, record);
            log.debug("Usage record saved: {}", context.getRequestId());
        } catch (Exception e) {
            log.error("Failed to save usage record", e);
        }
    }

    @Async
    public void recordAsync(ProviderInvokeContext context,
                            UsageMetrics usageMetrics,
                            long latencyMs,
                            String errorCode,
                            String errorMessage) {
        record(context, usageMetrics, latencyMs, errorCode, errorMessage);
    }

    public void recordError(ProviderInvokeContext context,
                            int responseStatus,
                            String errorCode,
                            String errorMessage,
                            long latencyMs) {
        if (context == null) {
            return;
        }
        UsageRecord record = buildRecord(context, null);
        record.setResponseStatus(responseStatus);
        record.setLatencyMs((int) latencyMs);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        try {
            usageRecordMapper.insert(record);
            log.debug("Error usage record saved: {}", context.getRequestId());
        } catch (Exception e) {
            log.error("Failed to save error usage record", e);
        }
    }

    private UsageRecord buildRecord(ProviderInvokeContext context, UsageMetrics usageMetrics) {
        UsageRecord record = new UsageRecord();
        record.setRequestId(context.getRequestId());
        record.setAccountId(context.getAccountId());
        record.setCustomerToken(context.getCustomerToken());
        record.setProviderToken(context.getProviderToken());
        record.setUsageType(context.getPlanId() == null ? "balance" : "plan");
        record.setUsageFrom(context.getPlanId() == null ? null : String.valueOf(context.getPlanId()));
        record.setProviderId(context.getProviderId());
        record.setProviderCode(context.getProviderCode());
        record.setProviderName(context.getProviderName());
        record.setEndpointCode(context.getEndpointCode());
        record.setModelId(context.getModelId());
        record.setModelCode(context.getModelCode());
        record.setModelName(context.getModelName());
        if (usageMetrics != null) {
            record.setPromptTokens(defaultInt(usageMetrics.getInputTokens()));
            record.setCompletionTokens(defaultInt(usageMetrics.getOutputTokens()));
            record.setTotalTokens(defaultInt(usageMetrics.getTotalTokens()));
        } else {
            record.setPromptTokens(0);
            record.setCompletionTokens(0);
            record.setTotalTokens(0);
        }
        BigDecimal promptCost = calculateCost(context.getInputPrice(), record.getPromptTokens());
        BigDecimal completionCost = calculateCost(context.getOutputPrice(), record.getCompletionTokens());
        record.setPromptCost(promptCost);
        record.setCompletionCost(completionCost);
        record.setTotalCost(promptCost.add(completionCost));
        return record;
    }

    private void consumePlanQuota(ProviderInvokeContext context, UsageRecord record) {
        if (context == null || record == null) {
            return;
        }
        Long planId = context.getPlanId();
        if (planId == null) {
            return;
        }
        if (record.getTotalCost() == null || record.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        customerPlanService.consumeQuota(planId, record.getTotalCost());
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal calculateCost(BigDecimal pricePerMillion, Integer tokens) {
        if (pricePerMillion == null || tokens == null) {
            return BigDecimal.ZERO;
        }
        return pricePerMillion
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
