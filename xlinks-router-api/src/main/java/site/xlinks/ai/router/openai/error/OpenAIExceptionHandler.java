package site.xlinks.ai.router.openai.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * Convert internal exceptions to OpenAI-compatible error payloads for /v1 APIs.
 *
 * Note: Streaming endpoints may write directly to response; those should handle errors in stream strategy.
 */
@Slf4j
@RestControllerAdvice(basePackages = "site.xlinks.ai.router.controller")
public class OpenAIExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<OpenAIErrorResponse> handleBusiness(BusinessException e) {
        log.warn("BusinessException: code={}, msg={}", e.getCode(), e.getMessage());
        // Map business errors to 400 by default; callers can refine later if needed.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(OpenAIErrorResponse.invalidRequest(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenAIErrorResponse> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OpenAIErrorResponse.internalError("Internal server error"));
    }
}
