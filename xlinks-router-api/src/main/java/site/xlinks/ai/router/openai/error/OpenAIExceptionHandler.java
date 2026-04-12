package site.xlinks.ai.router.openai.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<OpenAIErrorResponse> handleBusiness(BusinessException e) {
        log.warn("BusinessException: code={}, msg={}", e.getCode(), e.getMessage());
        HttpStatus status = resolveHttpStatus(e.getCode());
        OpenAIErrorResponse body = switch (status) {
            case UNAUTHORIZED -> OpenAIErrorResponse.unauthorized(e.getMessage());
            case INTERNAL_SERVER_ERROR -> OpenAIErrorResponse.internalError(e.getMessage());
            default -> OpenAIErrorResponse.invalidRequest(e.getMessage());
        };
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenAIErrorResponse> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OpenAIErrorResponse.internalError("Internal server error"));
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
