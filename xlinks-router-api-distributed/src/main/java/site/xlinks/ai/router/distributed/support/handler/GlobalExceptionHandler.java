package site.xlinks.ai.router.distributed.support.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.distributed.protocol.model.AnthropicProtocolErrorResponse;
import site.xlinks.ai.router.distributed.protocol.model.DistributedErrorCode;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.OpenAiProtocolErrorResponse;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogType;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.GLOBAL_BUSINESS_ERROR, ex.getCode(), ex.getMessage());
        RequestChainLogCollector.markBusinessFailure(ex.getMessage());
        return buildCommonErrorResponse(resolveHttpStatus(ex.getCode()), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> handleBadRequest(Exception ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.GLOBAL_BAD_REQUEST, ex.getMessage());
        RequestChainLogCollector.markBusinessFailure(ErrorCode.PARAM_ERROR.getMessage());
        return buildCommonErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.GLOBAL_NO_RESOURCE, request == null ? null : request.getRequestURI());
        RequestChainLogCollector.markBusinessFailure("请求路径不存在");
        if (isProtocolRequest(request)) {
            return buildProtocolNotFoundResponse(request);
        }
        return buildCommonErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.PARAM_ERROR.getCode(), "请求路径不存在");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.GLOBAL_UNEXPECTED_ERROR, ex.getMessage());
        RequestChainLogCollector.markUnexpectedFailure(ErrorCode.SYSTEM_ERROR.getMessage(), ex);
        if (isProtocolRequest(request)) {
            return buildProtocolErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
        return buildCommonErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.SYSTEM_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getMessage()
        );
    }

    private ResponseEntity<Result<Void>> buildCommonErrorResponse(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.error(code, message));
    }

    private ResponseEntity<String> buildProtocolNotFoundResponse(HttpServletRequest request) {
        String uri = request == null ? "" : request.getRequestURI();
        String message = String.format("Unsupported protocol endpoint: %s", uri);
        return buildProtocolErrorResponse(request, HttpStatus.NOT_FOUND, message);
    }

    private ResponseEntity<String> buildProtocolErrorResponse(HttpServletRequest request,
                                                              HttpStatus status,
                                                              String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (isAnthropicRequest(request)) {
            return new ResponseEntity<>(AnthropicProtocolErrorResponse.invalidRequest(message).toJson(), headers, status);
        }
        return new ResponseEntity<>(OpenAiProtocolErrorResponse.invalidRequest(message).toJson(), headers, status);
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

    private boolean isProtocolRequest(HttpServletRequest request) {
        String uri = request == null ? null : request.getRequestURI();
        return uri != null && uri.startsWith("/v1/");
    }

    private boolean isAnthropicRequest(HttpServletRequest request) {
        return request != null
                && ForwardProtocol.fromRequestUri(request.getRequestURI())
                .filter(protocol -> protocol == ForwardProtocol.ANTHROPIC_MESSAGES)
                .isPresent();
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
