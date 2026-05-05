package site.xlinks.ai.router.protocol.handler;

import jakarta.servlet.http.HttpServletRequest;
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
import site.xlinks.ai.router.protocol.controller.AnthropicProtocolController;
import site.xlinks.ai.router.protocol.controller.OpenAiProtocolController;
import site.xlinks.ai.router.protocol.model.AnthropicProtocolErrorResponse;
import site.xlinks.ai.router.protocol.model.DistributedErrorCode;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.OpenAiProtocolErrorResponse;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {OpenAiProtocolController.class, AnthropicProtocolController.class})
public class ProtocolExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.PROTOCOL_BUSINESS_ERROR, ex.getCode(), ex.getMessage());
        RequestChainLogCollector.markBusinessFailure(ex.getMessage());
        return buildProtocolErrorResponse(request, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.PROTOCOL_BAD_REQUEST, ex.getMessage());
        RequestChainLogCollector.markBusinessFailure("请求参数错误");
        return buildProtocolErrorResponse(request, ErrorCode.PARAM_ERROR.getCode(), "Invalid request body");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.PROTOCOL_UNEXPECTED_ERROR, ex.getMessage());
        RequestChainLogCollector.markUnexpectedFailure("协议请求出现未预期异常", ex);
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
        return request != null
                && ForwardProtocol.fromRequestUri(request.getRequestURI())
                .filter(protocol -> protocol == ForwardProtocol.ANTHROPIC_MESSAGES)
                .isPresent();
    }

    private HttpStatus resolveHttpStatus(int code) {
        if (isUnauthorizedCode(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (isNotImplementedCode(code)) {
            return HttpStatus.NOT_IMPLEMENTED;
        }
        if (isServerErrorCode(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String buildOpenAiBody(int code, String message) {
        if (isUnauthorizedCode(code)) {
            return OpenAiProtocolErrorResponse.unauthorized(message).toJson();
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return OpenAiProtocolErrorResponse.rateLimited(message).toJson();
        }
        if (isNotImplementedCode(code)) {
            return OpenAiProtocolErrorResponse.notImplemented(message).toJson();
        }
        if (isServerErrorCode(code)) {
            return OpenAiProtocolErrorResponse.internalError(message).toJson();
        }
        return OpenAiProtocolErrorResponse.invalidRequest(message).toJson();
    }

    private String buildAnthropicBody(int code, String message) {
        if (isUnauthorizedCode(code)) {
            return AnthropicProtocolErrorResponse.authenticationError(message).toJson();
        }
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return AnthropicProtocolErrorResponse.rateLimitError(message).toJson();
        }
        if (isNotImplementedCode(code)) {
            return AnthropicProtocolErrorResponse.notImplemented(message).toJson();
        }
        if (isServerErrorCode(code)) {
            return AnthropicProtocolErrorResponse.apiError(message).toJson();
        }
        return AnthropicProtocolErrorResponse.invalidRequest(message).toJson();
    }

    private boolean isUnauthorizedCode(int code) {
        return code == ErrorCode.UNAUTHORIZED.getCode()
                || code == DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getCode()
                || code == DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getCode();
    }

    private boolean isNotImplementedCode(int code) {
        return code == DistributedErrorCode.PROTOCOL_DEFINITION_ONLY.getCode();
    }

    private boolean isServerErrorCode(int code) {
        return code >= 5000;
    }
}
