package site.xlinks.ai.router.distributed.protocol.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import site.xlinks.ai.router.distributed.protocol.model.CustomerTokenSource;
import site.xlinks.ai.router.distributed.protocol.model.ProtocolRequestContext;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolCustomerTokenInterceptorTest {

    private final ProtocolCustomerTokenInterceptor interceptor =
            new ProtocolCustomerTokenInterceptor(new CustomerTokenResolver());

    @Test
    void shouldStoreResolvedBearerTokenInRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/chat/completions");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer customer-token-1");

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(accepted);
        CustomerTokenResolver.ResolvedCustomerToken resolvedToken = assertInstanceOf(
                CustomerTokenResolver.ResolvedCustomerToken.class,
                request.getAttribute(ProtocolRequestContext.ATTR_RESOLVED_CUSTOMER_TOKEN)
        );
        assertEquals("customer-token-1", resolvedToken.value());
        assertEquals(CustomerTokenSource.AUTHORIZATION_BEARER, resolvedToken.source());
    }

    @Test
    void shouldPreferAnthropicApiKeyWhenResolvingToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/messages");
        request.addHeader("x-api-key", "customer-token-2");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ignored-token");

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(accepted);
        assertEquals("customer-token-2", request.getAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN));
        assertEquals(CustomerTokenSource.X_API_KEY, request.getAttribute(ProtocolRequestContext.ATTR_CUSTOMER_TOKEN_SOURCE));
    }

    @Test
    void shouldStoreResolvedBearerTokenForResponsesEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/responses");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer customer-token-3");

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(accepted);
        CustomerTokenResolver.ResolvedCustomerToken resolvedToken = assertInstanceOf(
                CustomerTokenResolver.ResolvedCustomerToken.class,
                request.getAttribute(ProtocolRequestContext.ATTR_RESOLVED_CUSTOMER_TOKEN)
        );
        assertEquals("customer-token-3", resolvedToken.value());
        assertEquals(CustomerTokenSource.AUTHORIZATION_BEARER, resolvedToken.source());
    }
}
