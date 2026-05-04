package site.xlinks.ai.router.distributed.protocol.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.xlinks.ai.router.distributed.protocol.model.ProtocolRequestContext;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;

@Component
@RequiredArgsConstructor
public class ProtocolCustomerTokenInterceptor implements HandlerInterceptor {

    private final CustomerTokenResolver customerTokenResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/v1/")) {
            return true;
        }

        CustomerTokenResolver.ResolvedCustomerToken resolvedToken = isAnthropicRequest(uri)
                ? customerTokenResolver.resolveAnthropicToken(request)
                : customerTokenResolver.resolveOpenAiToken(request);

        request.setAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN, resolvedToken.value());
        request.setAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN_SOURCE, resolvedToken.source());
        return true;
    }

    private boolean isAnthropicRequest(String uri) {
        return "/v1/messages".equals(uri);
    }
}
