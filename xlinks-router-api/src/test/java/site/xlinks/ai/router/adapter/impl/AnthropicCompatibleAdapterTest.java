package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.service.ClientAbortException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals("Bearer provider-token", httpRequest.header("Authorization"));
        assertEquals("2023-06-01", httpRequest.header("anthropic-version"));
        assertEquals("prompt-caching-2024-07-31", httpRequest.header("anthropic-beta"));
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
    void shouldForwardDirectAnthropicSseEvents() {
        String ssePayload = String.join("\n",
                "id: evt-1",
                "event: message_start",
                "data: {\"type\":\"message_start\"}",
                "",
                "event: message_stop",
                "data: {\"type\":\"message_stop\"}",
                "",
                "");
        adapter = createAdapterResponding("text/event-stream; charset=utf-8", ssePayload);

        List<StreamEvent> events = new ArrayList<>();
        adapter.forwardStream(streamRequest(), context(), events::add);

        assertEquals(2, events.size());
        assertEquals("evt-1", events.get(0).getId());
        assertEquals("message_start", events.get(0).getEvent());
        assertEquals("{\"type\":\"message_start\"}", events.get(0).joinedData());
        assertEquals("message_stop", events.get(1).getEvent());
        assertEquals("{\"type\":\"message_stop\"}", events.get(1).joinedData());
    }

    @Test
    void shouldRejectNonSsePayloadForStreamRequests() {
        adapter = createAdapterResponding("application/json", "{\"type\":\"message_start\"}");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adapter.forwardStream(streamRequest(), context(), event -> {
                }));
        assertTrue(exception.getMessage().contains("did not return SSE"));
    }

    @Test
    void shouldRejectHtmlResponseWithHelpfulError() {
        IOException exception = assertThrows(IOException.class,
                () -> adapter.parseJsonResponseBody(
                        "<!doctype html><html><body>hello</body></html>",
                        "text/html; charset=utf-8",
                        "https://timicc.com/messages"));
        assertTrue(exception.getMessage().contains("returned HTML instead of JSON"));
        assertTrue(exception.getMessage().contains("https://timicc.com/messages"));
    }

    @Test
    void shouldAbortBeforeExecutingWhenStreamAlreadyCancelled() {
        ClientAbortException exception = assertThrows(
                ClientAbortException.class,
                () -> adapter.forwardStream(
                        streamRequest(),
                        context(),
                        event -> {
                        },
                        new AtomicBoolean(true)
                )
        );

        assertTrue(exception.getMessage().contains("cancelled before upstream call execution"));
    }

    private AnthropicCompatibleAdapter createAdapterResponding(String contentType, String body) {
        Interceptor interceptor = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", contentType)
                .body(ResponseBody.create(body, okhttp3.MediaType.get(contentType)))
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        return new AnthropicCompatibleAdapter(client, objectMapper);
    }

    private ProxyRequest streamRequest() {
        return ProxyRequest.builder()
                .protocol(ProxyProtocol.ANTHROPIC_MESSAGES)
                .stream(true)
                .model("claude-sonnet-4-5")
                .requestBody("{\"model\":\"claude-sonnet-4-5\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":true}")
                .build();
    }

    private ProviderInvokeContext context() {
        return ProviderInvokeContext.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .providerToken("provider-token")
                .providerModel("provider-model")
                .build();
    }
}
