package site.xlinks.ai.router.controller.strategy;

import site.xlinks.ai.router.dto.ChatCompletionRequest;

/**
 * Chat Completion 响应策略
 */
public interface ChatCompletionResponseStrategy<T> {

    /**
     * 是否支持当前请求
     */
    boolean supports(ChatCompletionRequest request);

    /**
     * 处理请求并返回响应对象
     */
    T handle(String token, String endpoint, ChatCompletionRequest request);
}
