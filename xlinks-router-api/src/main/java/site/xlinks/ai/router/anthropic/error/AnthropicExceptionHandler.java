package site.xlinks.ai.router.anthropic.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.controller.AnthropicProxyController;

/**
 * Convert internal exceptions to Anthropic-compatible error payloads for /v1/messages.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = AnthropicProxyController.class)
public class AnthropicExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AnthropicErrorResponse> handleBusiness(BusinessException e) {
        log.warn("Anthropic business error: code={}, msg={}", e.getCode(), e.getMessage());
        HttpStatus status = resolveHttpStatus(e.getCode());
        AnthropicErrorResponse body = switch (status) {
            case UNAUTHORIZED -> AnthropicErrorResponse.authenticationError(e.getMessage());
            case FORBIDDEN -> AnthropicErrorResponse.permissionError(e.getMessage());
            case INTERNAL_SERVER_ERROR -> AnthropicErrorResponse.apiError(e.getMessage());
            default -> AnthropicErrorResponse.invalidRequest(e.getMessage());
        };
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AnthropicErrorResponse> handleOther(Exception e) {
        log.error("Anthropic unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AnthropicErrorResponse.apiError("Internal server error"));
    }

    private HttpStatus resolveHttpStatus(int code) {
        if (code == ErrorCode.UNAUTHORIZED.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 5000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
