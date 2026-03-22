package site.xlinks.ai.router.controller.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.service.ChatService;

/**
 * Stream 模式响应策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamChatCompletionResponseStrategy implements ChatCompletionResponseStrategy<Void> {

    private final ChatService chatService;

    @Override
    public boolean supports(ChatCompletionRequest request) {
        return Boolean.TRUE.equals(request.getStream());
    }

    @Async
    @Override
    public Void handle(String token, String endpoint, ChatCompletionRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> log.debug("SSE completed for endpoint: {}", endpoint));
        emitter.onTimeout(() -> log.warn("SSE timeout for endpoint: {}", endpoint));
        try {
            chatService.chatCompletionsStream(token, endpoint, request, payload -> {
                try {
                    emitter.send(SseEmitter.event().data(payload));
                    if ("[DONE]".equals(payload)) {
                        emitter.complete();
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return null;
    }
}
