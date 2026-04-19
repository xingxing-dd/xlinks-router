package site.xlinks.ai.router.service.routing;

import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.ProxyProtocol;

/**
 * Centralized proxy/routing error factory to keep messages consistent across steps.
 */
public final class ProxyErrors {

    private ProxyErrors() {
    }

    public static BusinessException requestMustNotBeNull() {
        return new BusinessException(ErrorCode.PARAM_ERROR, "Request must not be null");
    }

    public static BusinessException modelMustNotBeBlank() {
        return new BusinessException(ErrorCode.PARAM_ERROR, "Model must not be blank");
    }

    public static BusinessException unsupportedProtocol() {
        return new BusinessException(ErrorCode.PARAM_ERROR, "Unsupported protocol");
    }

    public static BusinessException unsupportedProtocol(ProxyProtocol protocol) {
        return new BusinessException(ErrorCode.ROUTE_ERROR, "Unsupported protocol: " + protocol);
    }

    public static BusinessException missingBearerToken() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "Missing bearer token");
    }

    public static BusinessException invalidToken() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token");
    }

    public static BusinessException tokenDisabled() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "Token is disabled");
    }

    public static BusinessException tokenExpired() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "Token has expired");
    }

    public static BusinessException customerPlanUnavailable() {
        return new BusinessException(ErrorCode.FORBIDDEN, "Customer plan is unavailable");
    }

    public static BusinessException customerTokenTotalQuotaReached() {
        return new BusinessException(ErrorCode.FORBIDDEN, "Customer token total quota reached");
    }

    public static BusinessException customerTokenDailyQuotaReached() {
        return new BusinessException(ErrorCode.FORBIDDEN, "Customer token daily quota reached");
    }

    public static BusinessException customerTokenModelNotAllowed() {
        return new BusinessException(ErrorCode.FORBIDDEN, "Customer token model is not allowed");
    }

    public static BusinessException modelUnavailable(String modelCode) {
        return new BusinessException(ErrorCode.PARAM_ERROR, "Model does not exist or is unavailable: " + modelCode);
    }

    public static BusinessException noProviderMapping(String modelCode) {
        return new BusinessException(ErrorCode.ROUTE_ERROR,
                "No available provider mapping for model and protocol: " + modelCode);
    }

    public static BusinessException noProviderToken(String modelCode) {
        return new BusinessException(ErrorCode.ROUTE_ERROR,
                "No available provider token for model and protocol: " + modelCode);
    }

    public static BusinessException noProviderTokenAvailable() {
        return new BusinessException(ErrorCode.ROUTE_ERROR, "No available Provider Token");
    }

    public static BusinessException requestProcessingFailed(String detail) {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "Request processing failed: " + detail);
    }
}
