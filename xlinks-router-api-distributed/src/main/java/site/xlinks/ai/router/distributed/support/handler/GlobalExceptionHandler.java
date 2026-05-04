package site.xlinks.ai.router.distributed.support.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.Result;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception. uri={}, traceId={}, code={}, message={}",
                request.getRequestURI(), request.getHeader("X-Trace-Id"), ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request. uri={}, traceId={}, message={}",
                request.getRequestURI(), request.getHeader("X-Trace-Id"), ex.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception. uri={}, traceId={}",
                request.getRequestURI(), request.getHeader("X-Trace-Id"), ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
