package site.xlinks.ai.router.controller.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.service.ChatService;

/**
 * 非流式响应策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultChatCompletionResponseStrategy implements ChatCompletionResponseStrategy<ChatCompletionResponse> {

    private final ChatService chatService;

    @Override
    public boolean supports(ChatCompletionRequest request) {
        return request.getStream() == null || !request.getStream();
    }

    @Override
    public ChatCompletionResponse handle(String token, String endpoint, ChatCompletionRequest request) {
        return chatService.chatCompletions(token, endpoint, request);
    }
}
