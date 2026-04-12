package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapter;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Adapter for Anthropic-compatible /messages protocol.
 */
@Slf4j
@Component
public class AnthropicCompatibleAdapter extends AbstractSseHttpAdapter implements ProviderProtocolAdapter {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_FALLBACK_STREAM_PAYLOAD_CHARS = 200_000;

    private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
    private static final String HEADER_ANTHROPIC_BETA = "anthropic-beta";
    private static final String HEADER_X_API_KEY = "x-api-key";
    private static final String DEFAULT_ANTHROPIC_VERSION = "2023-06-01";

    private static final Set<String> ANTHROPIC_STREAM_EVENTS = Set.of(
            "message_start",
            "content_block_start",
            "content_block_delta",
            "content_block_stop",
            "message_delta",
            "message_stop",
            "ping",
            "error"
    );

    public AnthropicCompatibleAdapter(OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    @Override
    public boolean supports(ProxyProtocol protocol) {
        return protocol == ProxyProtocol.ANTHROPIC_MESSAGES;
    }

    @Override
    public JsonNode forward(ProxyRequest request, ProviderInvokeContext context) {
        try {
            Request httpRequest = buildRequest(request, context);
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw buildProviderFailure(response);
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }
                String responseJson = body.string();
                log.debug("Anthropic upstream response: {}", responseJson);
                return objectMapper.readTree(responseJson);
            }
        } catch (IOException e) {
            log.error("Error calling anthropic provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    @Override
    public void forwardStream(ProxyRequest request,
                              ProviderInvokeContext context,
                              Consumer<StreamEvent> onEvent) {
        try {
            Request httpRequest = buildRequest(request, context);
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw buildProviderFailure(response);
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }

                StreamReadResult readResult = readSseFrames(body, onEvent);
                if (readResult.emittedAnyEvent()) {
                    return;
                }

                String rawPayload = readResult.rawPayload();
                if (rawPayload == null || rawPayload.isBlank()) {
                    return;
                }
                if (rawPayload.length() > MAX_FALLBACK_STREAM_PAYLOAD_CHARS) {
                    emitFallbackErrorEvent(onEvent, "Upstream payload too large in stream mode");
                    return;
                }

                log.warn("Upstream returned non-Anthropic SSE payload in stream mode. bodyPreview={}",
                        abbreviate(rawPayload, 600));
                String eventName = extractAnthropicEventName(rawPayload);
                if (eventName == null) {
                    emitFallbackErrorEvent(onEvent, "Upstream returned non-Anthropic SSE payload in stream mode");
                    return;
                }
                onEvent.accept(StreamEvent.builder()
                        .event(eventName)
                        .dataLine(rawPayload)
                        .build());
            }
        } catch (IOException e) {
            log.error("Error calling anthropic provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    Request buildRequest(ProxyRequest request, ProviderInvokeContext context) {
        String url = context.getBaseUrl() + request.getProtocol().getProviderPath();
        String anthropicVersion = request.getPassthroughHeader(HEADER_ANTHROPIC_VERSION);
        if (anthropicVersion == null || anthropicVersion.isBlank()) {
            anthropicVersion = DEFAULT_ANTHROPIC_VERSION;
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", request.isStream() ? EVENT_STREAM_CONTENT_TYPE : "application/json")
                .addHeader(HEADER_X_API_KEY, context.getProviderToken())
                .addHeader(HEADER_ANTHROPIC_VERSION, anthropicVersion);
        String anthropicBeta = request.getPassthroughHeader(HEADER_ANTHROPIC_BETA);
        if (anthropicBeta != null && !anthropicBeta.isBlank()) {
            builder.addHeader(HEADER_ANTHROPIC_BETA, anthropicBeta);
        }
        return builder
                .post(RequestBody.create(rewriteRequestBody(request, context), JSON))
                .build();
    }

    String rewriteRequestBody(ProxyRequest request, ProviderInvokeContext context) {
        return rewriteModelAndStream(request, context);
    }

    private RuntimeException buildProviderFailure(Response response) throws IOException {
        String responseBody = response.body() == null ? "" : response.body().string();
        log.error("Anthropic API call failed: {} - {}, body={}", response.code(), response.message(), responseBody);
        return new RuntimeException("Provider API call failed: " + response.code());
    }

    private void emitFallbackErrorEvent(Consumer<StreamEvent> onEvent, String message) {
        onEvent.accept(StreamEvent.builder()
                .event("error")
                .dataLine(buildFallbackErrorJson(message))
                .build());
    }

    private String buildFallbackErrorJson(String message) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "error");
        ObjectNode error = root.putObject("error");
        error.put("type", "api_error");
        error.put("message", message == null || message.isBlank() ? "stream fallback error" : message);
        return writeJson(root);
    }

    private String writeJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return payload == null ? "{}" : payload.toString();
        }
    }

    private String extractAnthropicEventName(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path("type").asText();
            if (type == null || type.isBlank()) {
                return null;
            }
            return ANTHROPIC_STREAM_EVENTS.contains(type) ? type : null;
        } catch (Exception e) {
            return null;
        }
    }
}

