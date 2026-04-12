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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAICompatibleAdapterTest {

    private OpenAICompatibleAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new OpenAICompatibleAdapter(new OkHttpClient(), objectMapper);
    }

    @Test
    void shouldForceNonStreamRequestsToJsonModeAndRewriteModel() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .model("gpt-5.2-codex")
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\"}")
                .build();

        String rewrittenBody = adapter.rewriteRequestBody(request, context());
        JsonNode payload = objectMapper.readTree(rewrittenBody);

        assertEquals("provider-model", payload.path("model").asText());
        assertFalse(payload.path("stream").asBoolean(true));
    }

    @Test
    void shouldSetAcceptHeaderByRequestMode() {
        Request streamingRequest = adapter.buildRequest(ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(true)
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\",\"stream\":true}")
                .build(), context());
        Request nonStreamingRequest = adapter.buildRequest(ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(false)
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\",\"stream\":false}")
                .build(), context());

        assertEquals("text/event-stream", streamingRequest.header("Accept"));
        assertEquals("application/json", nonStreamingRequest.header("Accept"));
    }


    @Test
    void shouldUnwrapResponsesCompletedSsePayloadForNonStreamForward() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(false)
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\",\"stream\":false}")
                .build();
        String ssePayload = """
                event: response.output_text.delta
                data: {"type":"response.output_text.delta","delta":"Hi"}

                event: response.completed
                data: {"type":"response.completed","response":{"id":"resp_123","object":"response","usage":{"input_tokens":12,"output_tokens":4,"total_tokens":16}}}

                """;

        JsonNode response = adapter.parseResponseBody(request, ssePayload, "text/event-stream");

        assertEquals("resp_123", response.path("id").asText());
        assertEquals("response", response.path("object").asText());
        assertEquals(16, response.path("usage").path("total_tokens").asInt());
    }

    @Test
    void shouldParseRegularJsonResponseBody() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(false)
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\",\"stream\":false}")
                .build();

        JsonNode response = adapter.parseResponseBody(
                request,
                "{\"id\":\"resp_json\",\"object\":\"response\"}",
                "application/json"
        );

        assertTrue(response.has("id"));
        assertEquals("resp_json", response.path("id").asText());
    }

    @Test
    void shouldWrapResponsesFallbackPayloadToResponseCompletedWhenNoType() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(true)
                .model("gpt-5.4")
                .build();

        String rawPayload = """
                {"id":"resp_abc","object":"response","usage":{"input_tokens":2,"output_tokens":3,"total_tokens":5}}
                """;

        String fallbackData = ReflectionTestUtils.invokeMethod(adapter, "buildFallbackStreamData", request, rawPayload);
        JsonNode payload = objectMapper.readTree(fallbackData);
        assertEquals("response.completed", payload.path("type").asText());
        assertEquals("resp_abc", payload.path("response").path("id").asText());

        String eventName = ReflectionTestUtils.invokeMethod(adapter, "extractResponsesEventName", fallbackData);
        assertEquals("response.completed", eventName);
    }

    @Test
    void shouldKeepResponsesTypeWhenPayloadAlreadyHasType() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(true)
                .build();

        String rawPayload = """
                {"type":"response.output_text.delta","delta":"hello"}
                """;
        String fallbackData = ReflectionTestUtils.invokeMethod(adapter, "buildFallbackStreamData", request, rawPayload);
        JsonNode payload = objectMapper.readTree(fallbackData);

        assertEquals("response.output_text.delta", payload.path("type").asText());
        assertEquals("hello", payload.path("delta").asText());
    }

    @Test
    void shouldConvertChatCompletionToChatChunkForFallback() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.CHAT_COMPLETIONS)
                .stream(true)
                .model("gpt-5.4")
                .build();

        String rawPayload = """
                {"id":"chatcmpl_123","object":"chat.completion","created":1234567890,"model":"gpt-5.4","choices":[{"index":0,"message":{"role":"assistant","content":"Hi there"},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}
                """;

        String fallbackData = ReflectionTestUtils.invokeMethod(adapter, "buildFallbackStreamData", request, rawPayload);
        JsonNode payload = objectMapper.readTree(fallbackData);

        assertEquals("chat.completion.chunk", payload.path("object").asText());
        assertEquals("gpt-5.4", payload.path("model").asText());
        assertEquals("assistant", payload.path("choices").get(0).path("delta").path("role").asText());
        assertEquals("Hi there", payload.path("choices").get(0).path("delta").path("content").asText());
        assertEquals("stop", payload.path("choices").get(0).path("finish_reason").asText());
    }

    @Test
    void shouldEmitResponsesFallbackErrorAsSingleErrorEvent() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .stream(true)
                .build();

        List<StreamEvent> events = new ArrayList<>();
        Consumer<StreamEvent> collector = events::add;

        ReflectionTestUtils.invokeMethod(adapter, "emitFallbackErrorEvent", request, collector, "fallback failed");

        assertEquals(1, events.size());
        StreamEvent event = events.get(0);
        assertEquals("error", event.getEvent());
        JsonNode payload = objectMapper.readTree(event.joinedData());
        assertEquals("error", payload.path("type").asText());
        assertEquals("stream_fallback_error", payload.path("error").path("code").asText());
    }

    @Test
    void shouldEmitChatFallbackErrorThenDone() throws Exception {
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.CHAT_COMPLETIONS)
                .stream(true)
                .build();

        List<StreamEvent> events = new ArrayList<>();
        Consumer<StreamEvent> collector = events::add;

        ReflectionTestUtils.invokeMethod(adapter, "emitFallbackErrorEvent", request, collector, "fallback failed");

        assertEquals(2, events.size());
        StreamEvent first = events.get(0);
        assertNull(first.getEvent());
        JsonNode payload = objectMapper.readTree(first.joinedData());
        assertEquals("internal_error", payload.path("error").path("code").asText());
        assertEquals("[DONE]", events.get(1).joinedData());
    }

    private ProviderInvokeContext context() {
        return ProviderInvokeContext.builder()
                .baseUrl("https://example.com/v1")
                .providerToken("provider-token")
                .providerModel("provider-model")
                .build();
    }
}

