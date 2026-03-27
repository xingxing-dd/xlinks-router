package site.xlinks.ai.router.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.Result;

/**
 * Convert internal exceptions to OpenAI-compatible error payloads for /v1 APIs.
 *
 * Note: Streaming endpoints may write directly to response; those should handle errors in stream strategy.
 */
@Slf4j
@RestControllerAdvice(basePackages = "site.xlinks.ai.router.client.controller")
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusiness(BusinessException e) {
        log.warn("BusinessException: code={}, msg={}", e.getCode(), e.getMessage());
        // Map business errors to 400 by default; callers can refine later if needed.
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.error(500, "Internal server error"));
    }
}
