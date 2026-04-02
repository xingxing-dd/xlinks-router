package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.OpenAIProtocol;
import site.xlinks.ai.router.dto.OpenAIProxyRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        OpenAIProxyRequest request = OpenAIProxyRequest.builder()
                .protocol(OpenAIProtocol.RESPONSES)
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
        Request streamingRequest = adapter.buildRequest(OpenAIProxyRequest.builder()
                .protocol(OpenAIProtocol.RESPONSES)
                .stream(true)
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\",\"stream\":true}")
                .build(), context());
        Request nonStreamingRequest = adapter.buildRequest(OpenAIProxyRequest.builder()
                .protocol(OpenAIProtocol.RESPONSES)
                .stream(false)
                .requestBody("{\"model\":\"gpt-5.2-codex\",\"input\":\"hello\",\"stream\":false}")
                .build(), context());

        assertEquals("text/event-stream", streamingRequest.header("Accept"));
        assertEquals("application/json", nonStreamingRequest.header("Accept"));
    }

    @Test
    void shouldUnwrapResponsesCompletedSsePayloadForNonStreamForward() throws Exception {
        OpenAIProxyRequest request = OpenAIProxyRequest.builder()
                .protocol(OpenAIProtocol.RESPONSES)
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
        OpenAIProxyRequest request = OpenAIProxyRequest.builder()
                .protocol(OpenAIProtocol.RESPONSES)
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

    private ProviderInvokeContext context() {
        return ProviderInvokeContext.builder()
                .baseUrl("https://example.com/v1")
                .providerToken("provider-token")
                .providerModel("provider-model")
                .build();
    }
}
