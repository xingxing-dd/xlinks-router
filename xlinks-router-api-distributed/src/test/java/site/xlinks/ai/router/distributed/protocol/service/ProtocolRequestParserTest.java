package site.xlinks.ai.router.distributed.protocol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.model.CustomerTokenSource;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolRequestParserTest {

    private final ProtocolRequestParser parser = new ProtocolRequestParser(new ObjectMapper());

    @Test
    void shouldParseOpenAiRequestWithMinimalFields() {
        String rawBody = """
                {"model":"gpt-4o-mini","stream":true,"messages":[]}
                """;
        ForwardRequest request = parser.parseOpenAi(
                ForwardProtocol.CHAT_COMPLETIONS,
                rawBody,
                new CustomerTokenResolver.ResolvedCustomerToken("token-1", CustomerTokenSource.AUTHORIZATION_BEARER)
        );

        assertEquals("gpt-4o-mini", request.getModel());
        assertEquals(true, request.isStream());
        assertEquals(ForwardProtocol.CHAT_COMPLETIONS, request.getProtocol());
        assertEquals(rawBody, request.getRequestBody());
    }

    @Test
    void shouldParseAnthropicRequestAndCollectPassthroughHeaders() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("anthropic-version", "2023-06-01");
        servletRequest.addHeader("anthropic-beta", "tools-2024-04-04");
        String rawBody = """
                {"model":"claude-sonnet-4-20250514","messages":[]}
                """;

        ForwardRequest request = parser.parseAnthropic(
                rawBody,
                servletRequest,
                new CustomerTokenResolver.ResolvedCustomerToken("token-2", CustomerTokenSource.X_API_KEY)
        );

        assertEquals("claude-sonnet-4-20250514", request.getModel());
        assertFalse(request.isStream());
        assertEquals("2023-06-01", request.getPassthroughHeaders().get("anthropic-version"));
        assertEquals("tools-2024-04-04", request.getPassthroughHeaders().get("anthropic-beta"));
        assertEquals(rawBody, request.getRequestBody());
    }

    @Test
    void shouldRejectRequestWithoutModel() {
        assertThrows(BusinessException.class, () -> parser.parseOpenAi(
                ForwardProtocol.COMPLETIONS,
                """
                {"stream":false,"prompt":"hello"}
                """,
                new CustomerTokenResolver.ResolvedCustomerToken("token-3", CustomerTokenSource.AUTHORIZATION_BEARER)
        ));
    }
}
