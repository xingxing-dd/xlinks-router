package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicCompatibleAdapterTest {

    private AnthropicCompatibleAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new AnthropicCompatibleAdapter(new OkHttpClient(), objectMapper);
    }

    @Test
    void shouldSupportOnlyAnthropicMessagesProtocol() {
        assertTrue(adapter.supports(ProxyProtocol.ANTHROPIC_MESSAGES));
        assertFalse(adapter.supports(ProxyProtocol.CHAT_COMPLETIONS));
        assertFalse(adapter.supports(ProxyProtocol.RESPONSES));
    }

    @Test
    void shouldUseAnthropicHeadersForAnthropicMessages() {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.ANTHROPIC_MESSAGES)
                .stream(true)
                .model("claude-sonnet-4-5")
                .passthroughHeaders(Map.of(
                        "anthropic-version", "2023-06-01",
                        "anthropic-beta", "prompt-caching-2024-07-31"
                ))
                .requestBody("{\"model\":\"claude-sonnet-4-5\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":true}")
                .build();

        Request httpRequest = adapter.buildRequest(request, context());
        assertEquals("text/event-stream", httpRequest.header("Accept"));
        assertEquals("application/json", httpRequest.header("Content-Type"));
        assertEquals("provider-token", httpRequest.header("x-api-key"));
        assertEquals("2023-06-01", httpRequest.header("anthropic-version"));
        assertEquals("prompt-caching-2024-07-31", httpRequest.header("anthropic-beta"));
        assertNull(httpRequest.header("Authorization"));
    }

    @Test
    void shouldUseDefaultAnthropicVersionWhenMissingHeader() {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.ANTHROPIC_MESSAGES)
                .stream(false)
                .model("claude-sonnet-4-5")
                .requestBody("{\"model\":\"claude-sonnet-4-5\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":false}")
                .build();

        Request httpRequest = adapter.buildRequest(request, context());
        assertEquals("2023-06-01", httpRequest.header("anthropic-version"));
        assertNull(httpRequest.header("anthropic-beta"));
    }

    @Test
    void shouldRewriteAnthropicRequestBodyModelAndStream() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.ANTHROPIC_MESSAGES)
                .stream(true)
                .model("claude-sonnet-4-5")
                .requestBody("{\"model\":\"claude-sonnet-4-5\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":false}")
                .build();

        String rewritten = adapter.rewriteRequestBody(request, context());
        JsonNode payload = objectMapper.readTree(rewritten);
        assertEquals("provider-model", payload.path("model").asText());
        assertTrue(payload.path("stream").asBoolean(false));
    }

    @Test
    void shouldEmitAnthropicFallbackErrorWithoutDone() throws Exception {
        List<StreamEvent> events = new ArrayList<>();
        Consumer<StreamEvent> collector = events::add;
        ReflectionTestUtils.invokeMethod(adapter, "emitFallbackErrorEvent", collector, "fallback failed");

        assertEquals(1, events.size());
        StreamEvent event = events.get(0);
        assertEquals("error", event.getEvent());
        JsonNode payload = objectMapper.readTree(event.joinedData());
        assertEquals("error", payload.path("type").asText());
        assertEquals("api_error", payload.path("error").path("type").asText());
        assertEquals("fallback failed", payload.path("error").path("message").asText());
    }

    private ProviderInvokeContext context() {
        return ProviderInvokeContext.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .providerToken("provider-token")
                .providerModel("provider-model")
                .build();
    }
}

