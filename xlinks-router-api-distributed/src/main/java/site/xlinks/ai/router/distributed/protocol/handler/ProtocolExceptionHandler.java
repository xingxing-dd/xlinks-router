package site.xlinks.ai.router.distributed.protocol.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.controller.AnthropicProtocolController;
import site.xlinks.ai.router.distributed.protocol.controller.OpenAiProtocolController;
import site.xlinks.ai.router.distributed.protocol.model.AnthropicProtocolErrorResponse;
import site.xlinks.ai.router.distributed.protocol.model.DistributedErrorCode;
import site.xlinks.ai.router.distributed.protocol.model.OpenAiProtocolErrorResponse;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {OpenAiProtocolController.class, AnthropicProtocolController.class})
public class ProtocolExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Protocol business exception. uri={}, traceId={}, code={}, message={}",
                request.getRequestURI(), request.getHeader("X-Trace-Id"), ex.getCode(), ex.getMessage());
        return buildProtocolErrorResponse(request, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Protocol bad request. uri={}, traceId={}, message={}",
                request.getRequestURI(), request.getHeader("X-Trace-Id"), ex.getMessage());
        return buildProtocolErrorResponse(request, ErrorCode.PARAM_ERROR.getCode(), "Invalid request body");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Protocol unexpected exception. uri={}, traceId={}",
                request.getRequestURI(), request.getHeader("X-Trace-Id"), ex);
        return buildProtocolErrorResponse(request, ErrorCode.SYSTEM_ERROR.getCode(), "Internal server error");
    }

    private ResponseEntity<String> buildProtocolErrorResponse(HttpServletRequest request, int code, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (isAnthropicRequest(request)) {
            return new ResponseEntity<>(buildAnthropicBody(code, message), headers, resolveHttpStatus(code));
        }
        return new ResponseEntity<>(buildOpenAiBody(code, message), headers, resolveHttpStatus(code));
    }

    private boolean isAnthropicRequest(HttpServletRequest request) {
        return request != null && "/v1/messages".equals(request.getRequestURI());
    }

    private HttpStatus resolveHttpStatus(int code) {
        if (code == ErrorCode.UNAUTHORIZED.getCode()
                || code == DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getCode()
                || code == DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code == DistributedErrorCode.PROTOCOL_DEFINITION_ONLY.getCode()) {
            return HttpStatus.NOT_IMPLEMENTED;
        }
        if (code >= 5000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String buildOpenAiBody(int code, String message) {
        if (code == ErrorCode.UNAUTHORIZED.getCode()
                || code == DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getCode()
                || code == DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getCode()) {
            return OpenAiProtocolErrorResponse.unauthorized(message).toJson();
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return OpenAiProtocolErrorResponse.rateLimited(message).toJson();
        }
        if (code == DistributedErrorCode.PROTOCOL_DEFINITION_ONLY.getCode()) {
            return OpenAiProtocolErrorResponse.notImplemented(message).toJson();
        }
        if (code >= 5000) {
            return OpenAiProtocolErrorResponse.internalError(message).toJson();
        }
        return OpenAiProtocolErrorResponse.invalidRequest(message).toJson();
    }

    private String buildAnthropicBody(int code, String message) {
        if (code == ErrorCode.UNAUTHORIZED.getCode()
                || code == DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getCode()
                || code == DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getCode()) {
            return AnthropicProtocolErrorResponse.authenticationError(message).toJson();
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return AnthropicProtocolErrorResponse.rateLimitError(message).toJson();
        }
        if (code == DistributedErrorCode.PROTOCOL_DEFINITION_ONLY.getCode()) {
            return AnthropicProtocolErrorResponse.notImplemented(message).toJson();
        }
        if (code >= 5000) {
            return AnthropicProtocolErrorResponse.apiError(message).toJson();
        }
        return AnthropicProtocolErrorResponse.invalidRequest(message).toJson();
    }
}
