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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.xlinks.ai.router.anthropic.error.AnthropicErrorResponse;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.service.ClientAbortException;
import site.xlinks.ai.router.service.ProtocolProxyService;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Anthropic-compatible messages proxy endpoint.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@Tag(name = "Anthropic Proxy API", description = "Anthropic-compatible messages API")
public class AnthropicProxyController {

    private static final String HEADER_X_API_KEY = "x-api-key";
    private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
    private static final String HEADER_ANTHROPIC_BETA = "anthropic-beta";

    private final ProtocolProxyService proxyService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    @Value("${xlinks.router.sse.timeout-ms:900000}")
    private long sseTimeoutMs;

    public AnthropicProxyController(ProtocolProxyService proxyService,
                                    ObjectMapper objectMapper,
                                    @Qualifier("sseTaskExecutor") TaskExecutor taskExecutor) {
        this.proxyService = proxyService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/messages")
    @Operation(summary = "Anthropic Messages",
            description = "Forward Anthropic-compatible messages requests.")
    public Object messages(HttpServletRequest servletRequest,
                           HttpServletResponse servletResponse,
                           @RequestBody String requestBody) {
        ProxyRequest request = parseRequest(requestBody, servletRequest);
        String token = resolveCustomerToken(servletRequest);

        log.debug("Received anthropic/messages request, model={}, stream={}",
                request.getModel(), request.isStream());

        if (!request.isStream()) {
            return proxyService.forwardDirect(token, request);
        }
        return stream(token, request, servletResponse);
    }

    private ProxyRequest parseRequest(String requestBody, HttpServletRequest servletRequest) {
        try {
            JsonNode payload = objectMapper.readTree(requestBody);
            JsonNode modelNode = payload.path("model");
            JsonNode streamNode = payload.get("stream");

            Map<String, String> passthroughHeaders = new HashMap<>();
            String anthropicVersion = servletRequest.getHeader(HEADER_ANTHROPIC_VERSION);
            if (anthropicVersion != null && !anthropicVersion.isBlank()) {
                passthroughHeaders.put(HEADER_ANTHROPIC_VERSION, anthropicVersion);
            }
            String anthropicBeta = servletRequest.getHeader(HEADER_ANTHROPIC_BETA);
            if (anthropicBeta != null && !anthropicBeta.isBlank()) {
                passthroughHeaders.put(HEADER_ANTHROPIC_BETA, anthropicBeta);
            }

            return ProxyRequest.builder()
                    .protocol(ProxyProtocol.ANTHROPIC_MESSAGES)
                    .model(modelNode.isMissingNode() || modelNode.isNull() ? null : modelNode.asText())
                    .stream(streamNode == null || streamNode.isNull() ? null : streamNode.asBoolean())
                    .payload(payload)
                    .requestBody(requestBody)
                    .passthroughHeaders(passthroughHeaders)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid JSON request body");
        }
    }

    private String resolveCustomerToken(HttpServletRequest servletRequest) {
        String apiKey = servletRequest.getHeader(HEADER_X_API_KEY);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String authorization = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "Missing x-api-key header or Authorization: Bearer {token}");
        }
        String trimmed = authorization.trim();
        if (trimmed.length() < 7 || !trimmed.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "Invalid Authorization header, expected: Bearer {token}");
        }
        String token = trimmed.substring("Bearer".length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "Invalid Authorization header, expected: Bearer {token}");
        }
        return token;
    }

    private SseEmitter stream(String token, ProxyRequest request, HttpServletResponse servletResponse) {
        prepareSseResponseHeaders(servletResponse);
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        AtomicBoolean downstreamClosed = new AtomicBoolean(false);
        emitter.onCompletion(() -> log.debug("Anthropic SSE completed, protocol={}", request.getProtocol()));
        emitter.onCompletion(() -> downstreamClosed.set(true));
        emitter.onTimeout(() -> {
            downstreamClosed.set(true);
            log.warn("Anthropic SSE timeout, protocol={}", request.getProtocol());
            emitter.complete();
        });

        taskExecutor.execute(() -> {
            try {
                proxyService.forwardStream(token, request, event -> sendEvent(emitter, event), downstreamClosed);
                emitter.complete();
            } catch (ClientAbortException e) {
                downstreamClosed.set(true);
                log.info("Client disconnected from Anthropic SSE stream: {}", e.getMessage());
                emitter.complete();
            } catch (Exception e) {
                downstreamClosed.set(true);
                log.warn("Anthropic SSE forwarding failed: {}", e.getMessage(), e);
                sendErrorEventQuietly(emitter, e);
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
            if (event.getId() != null && !event.getId().isBlank()) {
                builder.id(event.getId());
            }
            if (event.getRetry() != null && event.getRetry() >= 0) {
                builder.reconnectTime(event.getRetry());
            }
            if (event.getEvent() != null && !event.getEvent().isBlank()) {
                builder.name(event.getEvent());
            }
            if (event.hasData()) {
                builder.data(event.joinedData());
            }
            emitter.send(builder);
        } catch (Exception e) {
            if (isClientDisconnected(e)) {
                throw new ClientAbortException("Failed to write anthropic SSE event", e);
            }
            throw new RuntimeException("Failed to write anthropic SSE event", e);
        }
    }

    private boolean isClientDisconnected(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ClientAbortException || current instanceof EOFException) {
                return true;
            }
            if (current instanceof IOException ioException) {
                String msg = ioException.getMessage();
                if (containsDisconnectKeywords(msg)) {
                    return true;
                }
            }
            String className = current.getClass().getName();
            if (className != null && className.toLowerCase(Locale.ROOT).contains("clientabortexception")) {
                return true;
            }
            String msg = current.getMessage();
            if (containsDisconnectKeywords(msg)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsDisconnectKeywords(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("broken pipe")
                || lower.contains("connection reset")
                || lower.contains("connection aborted")
                || lower.contains("connection closed")
                || lower.contains("forcibly closed")
                || lower.contains("stream closed");
    }

    private void sendErrorEventQuietly(SseEmitter emitter, Exception e) {
        try {
            AnthropicErrorResponse body = resolveStreamError(e);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(body.toJson()));
        } catch (Exception ignore) {
            // Ignore write failures on broken connections.
        }
    }

    private AnthropicErrorResponse resolveStreamError(Exception e) {
        String message = (e == null || e.getMessage() == null || e.getMessage().isBlank())
                ? "SSE stream failed"
                : e.getMessage();
        if (!(e instanceof BusinessException businessException)) {
            return AnthropicErrorResponse.apiError(message);
        }
        int code = businessException.getCode();
        if (code == ErrorCode.UNAUTHORIZED.getCode()) {
            return AnthropicErrorResponse.authenticationError(message);
        }
        if (code == ErrorCode.FORBIDDEN.getCode()) {
            return AnthropicErrorResponse.permissionError(message);
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return AnthropicErrorResponse.rateLimitError(message);
        }
        if (code == ErrorCode.UPSTREAM_TIMEOUT.getCode()) {
            return AnthropicErrorResponse.apiError(message);
        }
        if (code >= 5000) {
            return AnthropicErrorResponse.apiError(message);
        }
        return AnthropicErrorResponse.invalidRequest(message);
    }
}
