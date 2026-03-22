package site.xlinks.ai.router.controller.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.service.ChatService;

/**
 * Stream 任务异步执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamTaskExecutor {

    private final ChatService chatService;

    @Async
    public void execute(String token, String endpoint, ChatCompletionRequest request, SseEmitter emitter) {
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
        } catch (BusinessException e) {
            emitter.completeWithError(e);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
