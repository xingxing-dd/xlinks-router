package site.xlinks.ai.router.protocol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.protocol.model.CustomerTokenSource;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.protocol.model.ProtocolRequestContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolRequestParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolRequestParser parser = new ProtocolRequestParser(
            List.of(
                    new OpenAiProtocolRequestParsingStrategy(objectMapper),
                    new AnthropicProtocolRequestParsingStrategy(objectMapper)
            ),
            new ProtocolRequestContextResolver()
    );

    @Test
    void shouldParseOpenAiRequestWithMinimalFields() {
        MockHttpServletRequest servletRequest = requestWithResolvedToken(
                "token-1",
                CustomerTokenSource.AUTHORIZATION_BEARER
        );
        String rawBody = """
                {"model":"gpt-4o-mini","stream":true,"messages":[]}
                """;
        ForwardRequest request = parser.parse(
                ForwardProtocol.CHAT_COMPLETIONS,
                rawBody,
                servletRequest
        );

        assertEquals("gpt-4o-mini", request.getModel());
        assertEquals(true, request.isStream());
        assertEquals(ForwardProtocol.CHAT_COMPLETIONS, request.getProtocol());
        assertEquals(rawBody, request.getRequestBody());
        assertEquals("token-1", request.getCustomerToken());
    }

    @Test
    void shouldParseResponsesRequest() {
        MockHttpServletRequest servletRequest = requestWithResolvedToken(
                "token-resp",
                CustomerTokenSource.AUTHORIZATION_BEARER
        );
        String rawBody = """
                {"model":"gpt-5.4","stream":false,"input":"hello"}
                """;

        ForwardRequest request = parser.parse(
                ForwardProtocol.RESPONSES,
                rawBody,
                servletRequest
        );

        assertEquals(ForwardProtocol.RESPONSES, request.getProtocol());
        assertEquals("gpt-5.4", request.getModel());
        assertFalse(request.isStream());
        assertEquals(rawBody, request.getRequestBody());
        assertEquals("token-resp", request.getCustomerToken());
    }

    @Test
    void shouldParseAnthropicRequestAndCollectPassthroughHeaders() {
        MockHttpServletRequest servletRequest = requestWithResolvedToken(
                "token-2",
                CustomerTokenSource.X_API_KEY
        );
        servletRequest.addHeader("anthropic-version", "2023-06-01");
        servletRequest.addHeader("anthropic-beta", "tools-2024-04-04");
        String rawBody = """
                {"model":"claude-sonnet-4-20250514","messages":[]}
                """;

        ForwardRequest request = parser.parse(
                ForwardProtocol.ANTHROPIC_MESSAGES,
                rawBody,
                servletRequest
        );

        assertEquals("claude-sonnet-4-20250514", request.getModel());
        assertFalse(request.isStream());
        assertEquals("2023-06-01", request.getPassthroughHeaders().get("anthropic-version"));
        assertEquals("tools-2024-04-04", request.getPassthroughHeaders().get("anthropic-beta"));
        assertEquals(rawBody, request.getRequestBody());
        assertEquals(CustomerTokenSource.X_API_KEY, request.getTokenSource());
    }

    @Test
    void shouldRejectRequestWithoutModel() {
        MockHttpServletRequest servletRequest = requestWithResolvedToken(
                "token-3",
                CustomerTokenSource.AUTHORIZATION_BEARER
        );
        assertThrows(BusinessException.class, () -> parser.parse(
                ForwardProtocol.COMPLETIONS,
                """
                {"stream":false,"prompt":"hello"}
                """,
                servletRequest
        ));
    }

    @Test
    void shouldRejectRequestWithoutResolvedCustomerToken() {
        assertThrows(BusinessException.class, () -> parser.parse(
                ForwardProtocol.CHAT_COMPLETIONS,
                """
                {"model":"gpt-4o-mini"}
                """,
                new MockHttpServletRequest()
        ));
    }

    private MockHttpServletRequest requestWithResolvedToken(String token, CustomerTokenSource source) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                ProtocolRequestContext.ATTR_RESOLVED_CUSTOMER_TOKEN,
                new CustomerTokenResolver.ResolvedCustomerToken(token, source)
        );
        return request;
    }
}
