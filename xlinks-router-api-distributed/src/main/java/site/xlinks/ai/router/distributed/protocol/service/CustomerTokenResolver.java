package site.xlinks.ai.router.distributed.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.model.CustomerTokenSource;
import site.xlinks.ai.router.distributed.protocol.model.DistributedErrorCode;

@Component
public class CustomerTokenResolver {

    private static final String HEADER_X_API_KEY = "x-api-key";

    public ResolvedCustomerToken resolveOpenAiToken(HttpServletRequest request) {
        return resolveBearerToken(request);
    }

    public ResolvedCustomerToken resolveAnthropicToken(HttpServletRequest request) {
        String apiKey = StringUtils.trimToNull(request.getHeader(HEADER_X_API_KEY));
        if (apiKey != null) {
            return new ResolvedCustomerToken(apiKey, CustomerTokenSource.X_API_KEY);
        }
        return resolveBearerToken(request);
    }

    private ResolvedCustomerToken resolveBearerToken(HttpServletRequest request) {
        String authorization = StringUtils.trimToNull(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (authorization == null) {
            throw new BusinessException(
                    DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getCode(),
                    DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getMessage()
            );
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            throw new BusinessException(
                    DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getCode(),
                    DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getMessage()
            );
        }
        String token = StringUtils.trimToNull(authorization.substring("Bearer".length()));
        if (token == null) {
            throw new BusinessException(
                    DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getCode(),
                    DistributedErrorCode.INVALID_AUTHORIZATION_HEADER.getMessage()
            );
        }
        return new ResolvedCustomerToken(token, CustomerTokenSource.AUTHORIZATION_BEARER);
    }

    public record ResolvedCustomerToken(String value, CustomerTokenSource source) {
    }
}
