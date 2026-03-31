package site.xlinks.ai.router.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.OpenAIProtocol;
import site.xlinks.ai.router.dto.OpenAIProxyRequest;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;
import site.xlinks.ai.router.interceptor.BearerTokenInterceptor;
import site.xlinks.ai.router.service.OpenAIProxyService;

/**
 * HTTP entrypoint for OpenAI-compatible proxy APIs.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "OpenAI Proxy API", description = "OpenAI-compatible chat, responses, and models APIs")
public class OpenAIProxyController {

    private final OpenAIProxyService openAIProxyService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    @PostMapping("/{endpoint}/chat/completions")
    @Operation(summary = "Chat Completions",
            description = "Forward OpenAI-compatible chat/completions requests.")
    public Object chatCompletions(HttpServletRequest servletRequest,
                                  @PathVariable("endpoint") String endpoint,
                                  @RequestBody String requestBody) {
        return handleRequest(servletRequest, endpoint, requestBody, OpenAIProtocol.CHAT_COMPLETIONS);
    }

    @PostMapping("/{endpoint}/responses")
    @Operation(summary = "Responses",
            description = "Forward OpenAI-compatible responses requests.")
    public Object responses(HttpServletRequest servletRequest,
                            @PathVariable("endpoint") String endpoint,
                            @RequestBody String requestBody) {
        return handleRequest(servletRequest, endpoint, requestBody, OpenAIProtocol.RESPONSES);
    }

    @GetMapping("/models")
    @Operation(summary = "Models List", description = "Get the list of available models.")
    public Result<Object> modelsList(HttpServletRequest servletRequest) {
        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        try {
            return Result.success(openAIProxyService.listModels(token));
        } catch (BusinessException e) {
            log.warn("Models list error: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while listing models", e);
            return Result.error(500, "服务器内部错误");
        }
    }

    private Object handleRequest(HttpServletRequest servletRequest,
                                 String endpoint,
                                 String requestBody,
                                 OpenAIProtocol protocol) {
        OpenAIProxyRequest request = parseRequest(protocol, requestBody);
        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        log.info("Received {} request, endpoint={}, model={}, stream={}",
                protocol, endpoint, request.getModel(), request.isStream());

        if (!request.isStream()) {
            return openAIProxyService.forward(token, endpoint, request);
        }
        return stream(token, endpoint, request);
    }

    private OpenAIProxyRequest parseRequest(OpenAIProtocol protocol, String requestBody) {
        try {
            JsonNode payload = objectMapper.readTree(requestBody);
            JsonNode modelNode = payload.path("model");
            return OpenAIProxyRequest.builder()
                    .protocol(protocol)
                    .model(modelNode.isMissingNode() || modelNode.isNull() ? null : modelNode.asText())
                    .stream(payload.path("stream").asBoolean(false))
                    .requestBody(requestBody)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid JSON request body");
        }
    }

    private SseEmitter stream(String token, String endpoint, OpenAIProxyRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> log.debug("SSE completed for endpoint={}, protocol={}", endpoint, request.getProtocol()));
        emitter.onTimeout(() -> log.warn("SSE timeout for endpoint={}, protocol={}", endpoint, request.getProtocol()));

        taskExecutor.execute(() -> {
            try {
                openAIProxyService.forwardStream(token, endpoint, request, event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, OpenAIStreamEvent event) {
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event();
            if (event.getEvent() != null && !event.getEvent().isBlank()) {
                builder.name(event.getEvent());
            }
            if (event.getId() != null) {
                builder.id(event.getId());
            }
            if (event.getRetry() != null) {
                builder.reconnectTime(event.getRetry());
            }
            if (event.getComments() != null) {
                for (String comment : event.getComments()) {
                    builder.comment(comment);
                }
            }
            if (event.hasData()) {
                builder.data(event.joinedData());
            }
            emitter.send(builder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write SSE event", e);
        }
    }
}
