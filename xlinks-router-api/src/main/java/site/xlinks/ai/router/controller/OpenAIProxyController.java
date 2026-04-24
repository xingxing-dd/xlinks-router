package site.xlinks.ai.router.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.interceptor.BearerTokenInterceptor;
import site.xlinks.ai.router.openai.error.OpenAIErrorResponse;
import site.xlinks.ai.router.service.ClientAbortException;
import site.xlinks.ai.router.service.ProtocolProxyService;

import java.nio.charset.StandardCharsets;

/**
 * HTTP entrypoint for OpenAI-compatible proxy APIs.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@Tag(name = "OpenAI Proxy API", description = "OpenAI-compatible chat, responses, and models APIs")
public class OpenAIProxyController {

    private final ProtocolProxyService proxyService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    @Value("${xlinks.router.sse.timeout-ms:900000}")
    private long sseTimeoutMs;

    public OpenAIProxyController(ProtocolProxyService proxyService,
                                 ObjectMapper objectMapper,
                                 @Qualifier("sseTaskExecutor") TaskExecutor taskExecutor) {
        this.proxyService = proxyService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/chat/completions")
    @Operation(summary = "Chat Completions",
            description = "Forward OpenAI-compatible chat/completions requests.")
    public Object chatCompletions(HttpServletRequest servletRequest,
                                  HttpServletResponse servletResponse,
                                  @RequestBody String requestBody) {
        return handleRequest(servletRequest, servletResponse, requestBody, ProxyProtocol.CHAT_COMPLETIONS);
    }

    @PostMapping("/responses")
    @Operation(summary = "Responses",
            description = "Forward OpenAI-compatible responses requests.")
    public Object responses(HttpServletRequest servletRequest,
                            HttpServletResponse servletResponse,
                            @RequestBody String requestBody) {
        return handleRequest(servletRequest, servletResponse, requestBody, ProxyProtocol.RESPONSES);
    }

    @GetMapping("/models")
    @Operation(summary = "Models List", description = "Get the list of available models.")
    public Result<Object> modelsList(HttpServletRequest servletRequest) {
        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        try {
            return Result.success(proxyService.listModels(token));
        } catch (BusinessException e) {
            log.warn("Models list error: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while listing models", e);
            return Result.error(500, "Internal server error");
        }
    }

    private Object handleRequest(HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse,
                                 String requestBody,
                                 ProxyProtocol protocol) {
        ProxyRequest request = parseRequest(protocol, requestBody);
        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        log.debug("Received {} request, endpointCode={}, model={}, stream={}",
                protocol, protocol.getCode(), request.getModel(), request.isStream());

        if (!request.isStream()) {
            return proxyService.forwardDirect(token, request);
        }
        return stream(token, request, servletResponse);
    }

    private ProxyRequest parseRequest(ProxyProtocol protocol, String requestBody) {
        try {
            JsonNode payload = objectMapper.readTree(requestBody);
            JsonNode modelNode = payload.path("model");
            JsonNode streamNode = payload.get("stream");
            return ProxyRequest.builder()
                    .protocol(protocol)
                    .model(modelNode.isMissingNode() || modelNode.isNull() ? null : modelNode.asText())
                    .stream(streamNode == null || streamNode.isNull() ? null : streamNode.asBoolean())
                    .payload(payload)
                    .requestBody(requestBody)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid JSON request body");
        }
    }

    private SseEmitter stream(String token, ProxyRequest request, HttpServletResponse servletResponse) {
        prepareSseResponseHeaders(servletResponse);
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        emitter.onCompletion(() -> log.debug("SSE completed for endpointCode={}, protocol={}",
                request.getProtocol().getCode(), request.getProtocol()));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout for endpointCode={}, protocol={}",
                    request.getProtocol().getCode(), request.getProtocol());
            emitter.complete();
        });

        taskExecutor.execute(() -> {
            try {
                proxyService.forwardStream(token, request, event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (ClientAbortException e) {
                log.info("Client disconnected from OpenAI SSE stream: {}", e.getMessage());
                emitter.complete();
            } catch (Exception e) {
                log.warn("SSE forwarding failed for endpointCode={}, protocol={}: {}",
                        request.getProtocol().getCode(), request.getProtocol(), e.getMessage(), e);
                sendStreamErrorQuietly(emitter, request.getProtocol(), e);
                if (request.getProtocol() == ProxyProtocol.CHAT_COMPLETIONS) {
                    sendDoneEventQuietly(emitter);
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void prepareSseResponseHeaders(HttpServletResponse servletResponse) {
        if (servletResponse == null) {
            return;
        }
        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        servletResponse.setHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
        servletResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");
        servletResponse.setHeader("X-Accel-Buffering", "no");
    }

    private void sendEvent(SseEmitter emitter, StreamEvent event) {
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
            throw new ClientAbortException("Failed to write SSE event", e);
        }
    }

    private void sendStreamErrorQuietly(SseEmitter emitter, ProxyProtocol protocol, Exception e) {
        try {
            OpenAIErrorResponse errorResponse = resolveStreamError(e);
            if (protocol == ProxyProtocol.RESPONSES) {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(buildResponsesErrorEvent(errorResponse)));
                return;
            }
            emitter.send(SseEmitter.event().data(errorResponse.toJson()));
        } catch (Exception ignore) {
            // Ignore write failures on broken connections.
        }
    }

    private void sendDoneEventQuietly(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (Exception ignore) {
            // Ignore write failures on broken connections.
        }
    }

    private OpenAIErrorResponse resolveStreamError(Exception e) {
        String message = (e == null || e.getMessage() == null || e.getMessage().isBlank())
                ? "SSE stream failed"
                : e.getMessage();
        if (!(e instanceof BusinessException businessException)) {
            return OpenAIErrorResponse.internalError(message);
        }
        int code = businessException.getCode();
        if (code == ErrorCode.UNAUTHORIZED.getCode()) {
            return OpenAIErrorResponse.unauthorized(message);
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return OpenAIErrorResponse.rateLimited(message);
        }
        if (code == ErrorCode.UPSTREAM_TIMEOUT.getCode()) {
            return OpenAIErrorResponse.upstreamTimeout(message);
        }
        if (code >= 5000) {
            return OpenAIErrorResponse.internalError(message);
        }
        return OpenAIErrorResponse.invalidRequest(message);
    }

    private String buildResponsesErrorEvent(OpenAIErrorResponse errorResponse) {
        try {
            JsonNode errorNode = objectMapper.readTree(errorResponse.toJson()).path("error");
            com.fasterxml.jackson.databind.node.ObjectNode eventNode = objectMapper.createObjectNode();
            eventNode.put("type", "error");
            eventNode.set("error", errorNode.isMissingNode() ? objectMapper.createObjectNode() : errorNode);
            return objectMapper.writeValueAsString(eventNode);
        } catch (Exception ex) {
            return "{\"type\":\"error\",\"error\":{\"message\":\"Internal server error\",\"type\":\"server_error\",\"code\":\"internal_error\"}}";
        }
    }
}
