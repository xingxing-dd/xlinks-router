package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.entity.UsageRecord;

/**
 * 使用记录服务
 * 记录每次 API 调用的详细信息，用于计费和统计分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private final site.xlinks.ai.router.mapper.UsageRecordMapper usageRecordMapper;

    /**
     * 同步记录使用情况
     *
     * @param requestId       请求 ID
     * @param customerTokenId 客户 Token ID
     * @param providerId      Provider ID
     * @param modelId         模型 ID
     * @param providerTokenId Provider Token ID
     * @param requestModel    请求的模型名称
     * @param response        响应对象
     * @param latencyMs       延迟（毫秒）
     * @param errorCode       错误码（如果有）
     * @param errorMessage    错误信息（如果有）
     */
    public void record(String requestId, Long customerTokenId, Long providerId,
                       Long modelId, Long providerTokenId, String requestModel,
                       ChatCompletionResponse response, long latencyMs,
                       String errorCode, String errorMessage) {
        
        UsageRecord record = new UsageRecord();
        record.setRequestId(requestId);
        record.setCustomerTokenId(customerTokenId);
        record.setProviderId(providerId);
        record.setModelId(modelId);
        record.setProviderTokenId(providerTokenId);
        record.setRequestModel(requestModel);
        
        // 响应状态码：成功为 200，失败为具体错误码
        record.setResponseStatus(errorCode == null ? 200 : 500);

        // 填充使用统计
        if (response != null && response.getUsage() != null) {
            ChatCompletionResponse.Usage usage = response.getUsage();
            record.setPromptTokens(usage.getPromptTokens());
            record.setCompletionTokens(usage.getCompletionTokens());
            record.setTotalTokens(usage.getTotalTokens());
        } else {
            record.setPromptTokens(0);
            record.setCompletionTokens(0);
            record.setTotalTokens(0);
        }

        record.setLatencyMs((int) latencyMs);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);

        try {
            usageRecordMapper.insert(record);
            log.debug("Usage record saved: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to save usage record", e);
        }
    }

    /**
     * 异步记录使用情况
     * 调用完成后异步记录，不阻塞响应
     */
    @Async
    public void recordAsync(String requestId, Long customerTokenId, Long providerId,
                            Long modelId, Long providerTokenId, String requestModel,
                            ChatCompletionResponse response, long latencyMs,
                            String errorCode, String errorMessage) {
        record(requestId, customerTokenId, providerId, modelId, providerTokenId,
               requestModel, response, latencyMs, errorCode, errorMessage);
    }

    /**
     * 记录错误情况
     */
    public void recordError(String requestId, Long customerTokenId, Long providerId,
                           Long modelId, Long providerTokenId, String requestModel,
                           int responseStatus, String errorCode, String errorMessage, long latencyMs) {
        
        UsageRecord record = new UsageRecord();
        record.setRequestId(requestId);
        record.setCustomerTokenId(customerTokenId);
        record.setProviderId(providerId);
        record.setModelId(modelId);
        record.setProviderTokenId(providerTokenId);
        record.setRequestModel(requestModel);
        record.setResponseStatus(responseStatus);
        record.setPromptTokens(0);
        record.setCompletionTokens(0);
        record.setTotalTokens(0);
        record.setLatencyMs((int) latencyMs);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);

        try {
            usageRecordMapper.insert(record);
            log.debug("Error usage record saved: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to save error usage record", e);
        }
    }
}
