package site.xlinks.ai.router.distributed.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.model.CustomerTokenSource;
import site.xlinks.ai.router.distributed.protocol.model.DistributedErrorCode;
import site.xlinks.ai.router.distributed.protocol.model.ProtocolRequestContext;

@Component
public class ProtocolRequestContextResolver {

    public CustomerTokenResolver.ResolvedCustomerToken resolveCustomerToken(HttpServletRequest request) {
        Object resolvedToken = request.getAttribute(ProtocolRequestContext.ATTR_RESOLVED_CUSTOMER_TOKEN);
        if (resolvedToken instanceof CustomerTokenResolver.ResolvedCustomerToken token) {
            return token;
        }

        Object token = request.getAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN);
        Object tokenSource = request.getAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN_SOURCE);
        if (!(token instanceof String tokenValue) || tokenValue.isBlank() || !(tokenSource instanceof CustomerTokenSource source)) {
            throw new BusinessException(
                    DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getCode(),
                    DistributedErrorCode.MISSING_CUSTOMER_TOKEN.getMessage()
            );
        }
        return new CustomerTokenResolver.ResolvedCustomerToken(tokenValue, source);
    }
}
