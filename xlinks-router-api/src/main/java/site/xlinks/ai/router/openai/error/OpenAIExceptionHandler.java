package site.xlinks.ai.router.openai.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.controller.OpenAIProxyController;

/**
 * Convert internal exceptions to OpenAI-compatible error payloads for /v1 APIs.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = OpenAIProxyController.class)
public class OpenAIExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        log.warn("BusinessException: code={}, msg={}", e.getCode(), e.getMessage());
        HttpStatus status = resolveHttpStatus(e.getCode());
        OpenAIErrorResponse body = switch (status) {
            case UNAUTHORIZED -> OpenAIErrorResponse.unauthorized(e.getMessage());
            case TOO_MANY_REQUESTS -> OpenAIErrorResponse.rateLimited(e.getMessage());
            case GATEWAY_TIMEOUT -> OpenAIErrorResponse.upstreamTimeout(e.getMessage());
            case INTERNAL_SERVER_ERROR -> OpenAIErrorResponse.internalError(e.getMessage());
            default -> OpenAIErrorResponse.invalidRequest(e.getMessage());
        };
        return buildResponse(status, body, request, response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (isClientDisconnect(e)) {
            log.info("Client disconnected during OpenAI SSE response: {}", e.getMessage());
            return ResponseEntity.noContent().build();
        }
        log.error("Unhandled exception", e);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                OpenAIErrorResponse.internalError("Internal server error"),
                request,
                response
        );
    }

    private ResponseEntity<?> buildResponse(HttpStatus status,
                                            OpenAIErrorResponse body,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        if (isEventStreamRequest(request, response)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(body.toJson());
        }
        return ResponseEntity.status(status)
                .body(body);
    }

    private boolean isEventStreamRequest(HttpServletRequest request, HttpServletResponse response) {
        String responseContentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        String currentResponseContentType = response == null ? null : response.getContentType();
        return containsEventStream(responseContentType)
                || containsEventStream(accept)
                || containsEventStream(currentResponseContentType);
    }

    private boolean containsEventStream(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase().contains("text/event-stream");
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if (className != null) {
                String lowerClassName = className.toLowerCase();
                if (lowerClassName.contains("clientabortexception")
                        || lowerClassName.contains("asyncrequestnotusableexception")) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("broken pipe")
                        || lowerMessage.contains("connection reset")
                        || lowerMessage.contains("connection aborted")
                        || lowerMessage.contains("connection closed")
                        || lowerMessage.contains("forcibly closed")
                        || lowerMessage.contains("stream closed")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private HttpStatus resolveHttpStatus(int code) {
        if (code == ErrorCode.UNAUTHORIZED.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code == ErrorCode.UPSTREAM_TIMEOUT.getCode()) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (code >= 5000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
