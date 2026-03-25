package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.adapter.ChatProviderAdapter;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.dto.ChatCompletionResponse.Usage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容适配器
 * 使用 langchain4j 标准库进行非流式和流式调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleAdapter implements ChatProviderAdapter {

    private static final String PROVIDER_TYPE = "openai-compatible";
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String providerType) {
        return PROVIDER_TYPE.equalsIgnoreCase(providerType) || 
               "openai".equalsIgnoreCase(providerType);
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request, ProviderInvokeContext context) {
        // 构建 langchain4j ChatRequest
        ChatRequest chatRequest = buildChatRequest(request);

        // 使用 langchain4j 进行非流式调用
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(context.getBaseUrl())
                .apiKey(context.getProviderToken())
                .modelName(context.getProviderModel())
                .timeout(Duration.ofMinutes(30))
                .build();

        ChatResponse response = model.doChat(chatRequest);
        return convertToChatCompletionResponse(response);
    }

    @Override
    public void chatCompletionStream(ChatCompletionRequest request,
                                     ProviderInvokeContext context,
                                     Consumer<String> onEvent) {
        // 构建 langchain4j ChatRequest
        ChatRequest chatRequest = buildChatRequest(request);

        // 使用 langchain4j 进行流式调用
        StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
                .baseUrl(context.getBaseUrl())
                .apiKey(context.getProviderToken())
                .modelName(context.getProviderModel())
                .timeout(Duration.ofMinutes(30))
                .build();

        model.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 流式响应，转换为 SSE 格式
                String sseData = "data: " + partialResponse + "\n\n";
                onEvent.accept(sseData);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 最后计算 total token，发送完成标记
                log.debug("Stream completed: {}", completeResponse);
                onEvent.accept("data: [DONE]\n\n");
            }

            @Override
            public void onError(Throwable error) {
                log.error("Stream error", error);
                onEvent.accept("data: [ERROR] " + error.getMessage() + "\n\n");
            }
        });
    }

    /**
     * 将 ChatCompletionRequest 转换为 langchain4j ChatRequest
     */
    private ChatRequest buildChatRequest(ChatCompletionRequest request) {
        OpenAiChatRequestParameters parameters = OpenAiChatRequestParameters.builder()
                .temperature(request.getTemperature())
                .modelName(request.getModel())
                .maxCompletionTokens(request.getMaxTokens())
                .topP(request.getTopP())
                .frequencyPenalty(request.getFrequencyPenalty())
                .presencePenalty(request.getPresencePenalty())
                .build();
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatCompletionRequest.ChatMessage msg : request.getMessages()) {
            String role = msg.getRole();
            String content = msg.contentAsText();
            if ("user".equalsIgnoreCase(role)) {
                messages.add(UserMessage.from(content));
            }
            if ("system".equalsIgnoreCase(role)) {
                messages.add(SystemMessage.from(content));
            }
        }
        return ChatRequest.builder()
                .messages(messages.toArray(new ChatMessage[0]))
                .parameters(parameters)
                .build();
    }

    /**
     * 将 langchain4j ChatResponse 转换为标准 ChatCompletionResponse
     * 使用 objectMapper 解析响应内容
     */
    private ChatCompletionResponse convertToChatCompletionResponse(ChatResponse response) {
        ChatCompletionResponse result = new ChatCompletionResponse();
        result.setId("chatcmpl-" + System.currentTimeMillis());
        result.setObject("chat.completion");
        result.setCreated(System.currentTimeMillis() / 1000);
        result.setModel(response.aiMessage() != null ? "assistant" : "gpt");

        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(new ChatCompletionResponse.Message());
        choice.getMessage().setRole("assistant");
        // 使用 objectMapper 解析 AI 消息内容
        String content = extractContent(response.aiMessage());
        choice.getMessage().setContent(content);
        choice.setFinishReason("stop");
        result.setChoices(List.of(choice));

        // 设置 usage 信息
        if (response.tokenUsage() != null) {
            Usage usage = new Usage();
            usage.setPromptTokens(response.tokenUsage().inputTokenCount());
            usage.setCompletionTokens(response.tokenUsage().outputTokenCount());
            usage.setTotalTokens(response.tokenUsage().totalTokenCount());
            result.setUsage(usage);
        }

        return result;
    }

    /**
     * 从 AI Message 中提取文本内容
     */
    @SuppressWarnings("unchecked")
    private String extractContent(dev.langchain4j.data.message.AiMessage aiMessage) {
        if (aiMessage == null) {
            return "";
        }
        try {
            // 将 AI 消息序列化为 JSON，然后反序列化为 Map 来提取内容
            String json = objectMapper.writeValueAsString(aiMessage);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Object singleText = map.get("singleText");
            if (singleText != null) {
                return singleText.toString();
            }
            // 如果没有 singleText，尝试从 AI 消息的字符串表示中提取
            return aiMessage.toString();
        } catch (Exception e) {
            log.warn("Failed to extract content from AI message, using toString", e);
            return aiMessage.toString();
        }
    }
}
