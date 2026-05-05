package site.xlinks.ai.router.protocol.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ProtocolRequestContext;
import site.xlinks.ai.router.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

@Component
@RequiredArgsConstructor
public class ProtocolCustomerTokenInterceptor implements HandlerInterceptor {

    private final CustomerTokenResolver customerTokenResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }

        ForwardProtocol protocol = ForwardProtocol.fromRequestUri(uri).orElse(null);
        if (protocol == null) {
            return true;
        }

        CustomerTokenResolver.ResolvedCustomerToken resolvedToken = customerTokenResolver.resolve(request, protocol);
        RequestChainLogCollector.bindProtocolContext(protocol.getCode(), resolvedToken.source().name(), null);
        RequestChainLogCollector.record(RequestChainLogType.CUSTOMER_TOKEN_RESOLVED, protocol.getCode(), resolvedToken.source());

        request.setAttribute(ProtocolRequestContext.ATTR_RESOLVED_CUSTOMER_TOKEN, resolvedToken);
        request.setAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN, resolvedToken.value());
        request.setAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN_SOURCE, resolvedToken.source());
        return true;
    }
}
