package site.xlinks.ai.router.distributed.protocol.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.model.CustomerTokenSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTokenResolverTest {

    private final CustomerTokenResolver resolver = new CustomerTokenResolver();

    @Test
    void shouldResolveOpenAiBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer customer-token-1");

        CustomerTokenResolver.ResolvedCustomerToken resolved = resolver.resolveOpenAiToken(request);

        assertEquals("customer-token-1", resolved.value());
        assertEquals(CustomerTokenSource.AUTHORIZATION_BEARER, resolved.source());
    }

    @Test
    void shouldResolveAnthropicApiKeyFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-api-key", "customer-token-2");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ignored-token");

        CustomerTokenResolver.ResolvedCustomerToken resolved = resolver.resolveAnthropicToken(request);

        assertEquals("customer-token-2", resolved.value());
        assertEquals(CustomerTokenSource.X_API_KEY, resolved.source());
    }

    @Test
    void shouldRejectMissingToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(BusinessException.class, () -> resolver.resolveOpenAiToken(request));
    }
}
