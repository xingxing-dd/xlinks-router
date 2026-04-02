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

    @PostMapping("/chat/completions")
    @Operation(summary = "Chat Completions",
            description = "Forward OpenAI-compatible chat/completions requests.")
    public Object chatCompletions(HttpServletRequest servletRequest,
                                  @RequestBody String requestBody) {
        return handleRequest(servletRequest, requestBody, OpenAIProtocol.CHAT_COMPLETIONS);
    }

    @PostMapping("/responses")
    @Operation(summary = "Responses",
            description = "Forward OpenAI-compatible responses requests.")
    public Object responses(HttpServletRequest servletRequest,
                            @RequestBody String requestBody) {
        return handleRequest(servletRequest, requestBody, OpenAIProtocol.RESPONSES);
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
                                 String requestBody,
                                 OpenAIProtocol protocol) {
        OpenAIProxyRequest request = parseRequest(protocol, requestBody);
        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        log.info("Received {} request, endpointCode={}, model={}, stream={}",
                protocol, protocol.getCode(), request.getModel(), request.isStream());

        if (!request.isStream()) {
            return openAIProxyService.forward(token, request);
        }
        return stream(token, request);
    }

    private OpenAIProxyRequest parseRequest(OpenAIProtocol protocol, String requestBody) {
        try {
            JsonNode payload = objectMapper.readTree(requestBody);
            JsonNode modelNode = payload.path("model");
            JsonNode streamNode = payload.get("stream");
            return OpenAIProxyRequest.builder()
                    .protocol(protocol)
                    .model(modelNode.isMissingNode() || modelNode.isNull() ? null : modelNode.asText())
                    .stream(streamNode == null || streamNode.isNull() ? null : streamNode.asBoolean())
                    .requestBody(requestBody)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid JSON request body");
        }
    }

    private SseEmitter stream(String token, OpenAIProxyRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> log.debug("SSE completed for endpointCode={}, protocol={}",
                request.getProtocol().getCode(), request.getProtocol()));
        emitter.onTimeout(() -> log.warn("SSE timeout for endpointCode={}, protocol={}",
                request.getProtocol().getCode(), request.getProtocol()));

        taskExecutor.execute(() -> {
            try {
                openAIProxyService.forwardStream(token, request, event -> sendEvent(emitter, event));
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
            String eventName = event.getEvent();
            if (eventName != null && !eventName.isBlank()) {
                builder.name(eventName);
            }
            String id = event.getId();
            if (id != null && !id.isBlank()) {
                builder.id(id);
            }
            if (event.getRetry() != null) {
                builder.reconnectTime(event.getRetry());
            }
            if (event.getComments() != null) {
                for (String comment : event.getComments()) {
                    if (comment != null) {
                        builder.comment(comment);
                    }
                }
            }
            if (event.hasData()) {
                String data = event.joinedData();
                if (data != null) {
                    builder.data(data);
                }
            }
            emitter.send(builder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write SSE event", e);
        }
    }
}
