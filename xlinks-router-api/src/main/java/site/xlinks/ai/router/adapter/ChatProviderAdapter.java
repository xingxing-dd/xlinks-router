package site.xlinks.ai.router.adapter;

import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;

/**
 * Chat Provider 适配器接口
 * 用于支持不同类型的 Provider（OpenAI 兼容、Azure、Anthropic 等）
 * 
 * 扩展点：新增 Provider 类型时，只需实现此接口并注册为 Spring Bean
 */
public interface ChatProviderAdapter {

    /**
     * 判断是否支持指定的 Provider 类型
     *
     * @param providerType Provider 类型
     * @return 是否支持
     */
    boolean supports(String providerType);

    /**
     * 执行 Chat Completion 请求（非流式）
     *
     * @param request  请求对象
     * @param context  调用上下文
     * @return 响应对象
     */
    ChatCompletionResponse chatCompletion(ChatCompletionRequest request, ProviderInvokeContext context);

    /**
     * 执行 Chat Completion 请求（流式）
     *
     * @param request  请求对象
     * @param context  调用上下文
     * @param onEvent  逐条事件回调（仅传递 data payload）
     */
    default void chatCompletionStream(ChatCompletionRequest request,
                                      ProviderInvokeContext context,
                                      java.util.function.Consumer<String> onEvent) {
        throw new UnsupportedOperationException("Stream not supported yet");
    }
}
