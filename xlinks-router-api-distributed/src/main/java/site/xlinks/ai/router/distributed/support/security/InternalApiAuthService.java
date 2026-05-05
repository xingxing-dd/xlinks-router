package site.xlinks.ai.router.distributed.support.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * 内部管理接口鉴权。
 */
@Slf4j
@Service
public class InternalApiAuthService {

    @Value("${xlinks.router.internal.auth-token:}")
    private String internalAuthToken;

    public void validateCacheRefreshAuthorization(String authorization) {
        String token = parseBearerToken(authorization);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少内部访问令牌");
        }
        if (internalAuthToken == null || internalAuthToken.isBlank()) {
            log.error("内部缓存刷新令牌未配置");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "内部缓存刷新令牌未配置");
        }
        if (!internalAuthToken.equals(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "内部访问令牌无效");
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
