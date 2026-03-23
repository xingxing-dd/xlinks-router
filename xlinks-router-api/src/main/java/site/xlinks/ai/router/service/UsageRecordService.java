package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.entity.UsageRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 使用记录服务
 * 记录每次 API 调用的详细信息，用于计费和统计分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private final site.xlinks.ai.router.mapper.UsageRecordMapper usageRecordMapper;
    private final CustomerPlanService customerPlanService;

    /**
     * 同步记录使用情况
     *
     * @param context      调用上下文
     * @param response     响应对象
     * @param latencyMs    延迟（毫秒）
     * @param errorCode    错误码（如果有）
     * @param errorMessage 错误信息（如果有）
     */
    public void record(ProviderInvokeContext context,
                       ChatCompletionResponse response,
                       long latencyMs,
                       String errorCode,
                       String errorMessage) {
        if (context == null) {
            return;
        }
        UsageRecord record = buildRecord(context, response);
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

    /**
     * 异步记录使用情况
     * 调用完成后异步记录，不阻塞响应
     */
    @Async
    public void recordAsync(ProviderInvokeContext context,
                            ChatCompletionResponse response,
                            long latencyMs,
                            String errorCode,
                            String errorMessage) {
        record(context, response, latencyMs, errorCode, errorMessage);
    }

    /**
     * 记录错误情况
     */
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

    private UsageRecord buildRecord(ProviderInvokeContext context, ChatCompletionResponse response) {
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
        if (response != null && response.getUsage() != null) {
            ChatCompletionResponse.Usage usage = response.getUsage();
            record.setPromptTokens(defaultInt(usage.getPromptTokens()));
            record.setCompletionTokens(defaultInt(usage.getCompletionTokens()));
            record.setTotalTokens(defaultInt(usage.getTotalTokens()));
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
