package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.adapter.ChatProviderAdapter;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容适配器
 * 支持 DeepSeek、OpenAI、Local LLM 等兼容 OpenAI API 的服务
 * 
 * 扩展点：新增 OpenAI 兼容的 Provider 时，只需在数据库配置 provider_type = 'openai-compatible'
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleAdapter implements ChatProviderAdapter {

    private static final String PROVIDER_TYPE = "openai-compatible";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String providerType) {
        return PROVIDER_TYPE.equalsIgnoreCase(providerType) || 
               "openai".equalsIgnoreCase(providerType);
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request, ProviderInvokeContext context) {
        try {
            // 构建请求 URL
            String url = context.getBaseUrl() + "/chat/completions";

            // 构建请求体（将模型名替换为底层模型名）
            ChatCompletionRequest adaptedRequest = adaptRequest(request, context.getProviderModel(), false);

            String requestJson = objectMapper.writeValueAsString(adaptedRequest);

            // 构建 HTTP 请求
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + context.getProviderToken())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, JSON))
                    .build();

            // 执行请求
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Provider API call failed: {} - {}", response.code(), response.message());
                    throw new RuntimeException("Provider API call failed: " + response.code());
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }

                String responseJson = body.string();
                log.info("========>{}", responseJson);
                return objectMapper.readValue(responseJson, ChatCompletionResponse.class);
            }
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatCompletionStream(ChatCompletionRequest request,
                                     ProviderInvokeContext context,
                                     Consumer<String> onEvent) {
        String url = context.getBaseUrl() + "/v1/chat/completions";
        ChatCompletionRequest adaptedRequest = adaptRequest(request, context.getProviderModel(), true);
        try {
            String requestJson = objectMapper.writeValueAsString(adaptedRequest);
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + context.getProviderToken())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Provider Stream API call failed: {} - {}", response.code(), response.message());
                    throw new RuntimeException("Provider API call failed: " + response.code());
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }
                BufferedSource source = body.source();
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    onEvent.accept(payload);
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    /**
     * 适配请求参数
     * 将客户请求的模型名替换为底层实际的模型名
     */
    private ChatCompletionRequest adaptRequest(ChatCompletionRequest request, String providerModel, boolean stream) {
        // 创建副本并替换模型名
        ChatCompletionRequest adapted = new ChatCompletionRequest();
        adapted.setModel(providerModel);
        adapted.setMessages(request.getMessages());
        adapted.setTemperature(request.getTemperature());
        adapted.setMaxTokens(request.getMaxTokens());
        adapted.setTopP(request.getTopP());
        adapted.setFrequencyPenalty(request.getFrequencyPenalty());
        adapted.setPresencePenalty(request.getPresencePenalty());
        adapted.setStop(request.getStop());
        adapted.setStream(stream);
        adapted.setExtraParams(request.getExtraParams());
        return adapted;
    }
}
