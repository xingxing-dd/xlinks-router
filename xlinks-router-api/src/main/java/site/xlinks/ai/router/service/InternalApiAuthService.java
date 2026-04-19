package site.xlinks.ai.router.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * Authentication for internal management APIs.
 */
@Slf4j
@Service
public class InternalApiAuthService {

    @Value("${xlinks.router.auth.internal-cache-refresh-token:}")
    private String internalCacheRefreshToken;

    public void validateCacheRefreshAuthorization(String authorization) {
        String token = parseBearerToken(authorization);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        if (internalCacheRefreshToken == null || internalCacheRefreshToken.isBlank()) {
            log.error("Internal cache refresh token is not configured");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Internal cache refresh token is not configured");
        }
        if (!internalCacheRefreshToken.equals(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid internal access token");
        }
    }

    public String expectedHeaderName() {
        return HttpHeaders.AUTHORIZATION;
    }

    private String parseBearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        String trimmed = authorization.trim();
        if (trimmed.length() < 7) {
            return null;
        }
        if (!trimmed.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            return null;
        }
        return trimmed.substring("Bearer".length()).trim();
    }
}
