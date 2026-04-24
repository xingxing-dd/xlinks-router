package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
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
import site.xlinks.ai.router.openai.error.OpenAIErrorResponse;
import site.xlinks.ai.router.service.StreamFirstResponseTimeoutException;
import site.xlinks.ai.router.service.UpstreamTimeoutException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.function.Consumer;

/**
 * Adapter for providers exposing OpenAI-compatible chat/responses APIs.
 */
@Slf4j
@Component
public class OpenAICompatibleAdapter extends AbstractSseHttpAdapter implements ProviderProtocolAdapter {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_FALLBACK_STREAM_PAYLOAD_CHARS = 200_000;

    public OpenAICompatibleAdapter(OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    @Override
    public boolean supports(ProxyProtocol protocol) {
        return protocol == ProxyProtocol.CHAT_COMPLETIONS
                || protocol == ProxyProtocol.RESPONSES;
    }

    @Override
    public JsonNode forwardDirect(ProxyRequest request, ProviderInvokeContext context) {
        try {
            Request httpRequest = buildRequest(request, context);
            Call call = createScopedClient(context, false).newCall(httpRequest);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw buildProviderFailure(response);
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }

                String contentType = response.header("Content-Type", "");
                String responseJson = body.string();
                log.debug("Upstream {} response: {}", request.getProtocol(), responseJson);
                return parseResponseBody(request, responseJson, contentType);
            }
        } catch (InterruptedIOException e) {
            throw new UpstreamTimeoutException("Upstream request timed out", e);
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    @Override
    public void forwardStream(ProxyRequest request,
                              ProviderInvokeContext context,
                              Consumer<StreamEvent> onEvent) {
        try {
            Request httpRequest = buildRequest(request, context);
            Call call = createScopedClient(context, true).newCall(httpRequest);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw buildProviderFailure(response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }

                String contentType = response.header("Content-Type", "");
                StreamReadResult readResult = readSseFrames(body, onEvent);
                if (readResult.emittedAnyEvent()) {
                    return;
                }

                String rawPayload = readResult.rawPayload();
                if (rawPayload == null || rawPayload.isEmpty()) {
                    return;
                }
                if (rawPayload.length() > MAX_FALLBACK_STREAM_PAYLOAD_CHARS) {
                    log.warn("Fallback stream payload too large, protocol={}, size={}",
                            request == null ? null : request.getProtocol(), rawPayload.length());
                    emitFallbackErrorEvent(request, onEvent, "Upstream payload too large in stream mode");
                    return;
                }

                log.warn("Upstream returned non-SSE payload in stream mode. protocol={}, contentType={}, bodyPreview={}",
                        request == null ? null : request.getProtocol(),
                        contentType,
                        abbreviate(rawPayload, 600));
                String fallbackData = buildFallbackStreamData(request, rawPayload);
                ProxyProtocol protocol = request == null ? null : request.getProtocol();
                if (protocol == ProxyProtocol.RESPONSES) {
                    String eventName = extractResponsesEventName(fallbackData);
                    StreamEvent.StreamEventBuilder builder = StreamEvent.builder()
                            .dataLine(fallbackData);
                    if (eventName != null && !eventName.isBlank()) {
                        builder.event(eventName);
                    }
                    onEvent.accept(builder.build());
                    return;
                }

                onEvent.accept(StreamEvent.builder().dataLine(fallbackData).build());
                onEvent.accept(StreamEvent.builder().dataLine("[DONE]").build());
            }
        } catch (InterruptedIOException e) {
            throw new StreamFirstResponseTimeoutException("Stream first response timeout", e);
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    Request buildRequest(ProxyRequest request, ProviderInvokeContext context) {
        String url = context.getBaseUrl() + request.getProtocol().getProviderPath();
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + context.getProviderToken())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", request.isStream() ? EVENT_STREAM_CONTENT_TYPE : "application/json")
                .post(RequestBody.create(rewriteRequestBody(request, context), JSON))
                .build();
    }

    String rewriteRequestBody(ProxyRequest request, ProviderInvokeContext context) {
        return rewriteModelAndStream(request, context);
    }

    private RuntimeException buildProviderFailure(Response response) throws IOException {
        String responseBody = response.body() == null ? "" : response.body().string();
        log.error("Provider API call failed: {} - {}, body={}", response.code(), response.message(), responseBody);
        return new RuntimeException("Provider API call failed: " + response.code());
    }

    JsonNode parseResponseBody(ProxyRequest request,
                               String responseBody,
                               String contentType) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("Empty response from provider");
        }
        if (isEventStream(contentType) || looksLikeSse(responseBody)) {
            return parseSseResponseBody(request, responseBody);
        }
        return objectMapper.readTree(responseBody);
    }

    private JsonNode parseSseResponseBody(ProxyRequest request, String responseBody) throws IOException {
        JsonNode fallbackPayload = null;
        for (StreamEvent event : parseEvents(responseBody)) {
            if (!event.hasData()) {
                continue;
            }
            String data = event.joinedData();
            if (data == null || data.isBlank() || "[DONE]".equals(data)) {
                continue;
            }
            JsonNode payload = objectMapper.readTree(data);
            if (request != null && request.getProtocol() == ProxyProtocol.RESPONSES) {
                JsonNode responseNode = payload.get("response");
                String eventType = event.getEvent();
                if ((eventType != null && "response.completed".equals(eventType))
                        || "response.completed".equals(payload.path("type").asText())) {
                    return responseNode != null && !responseNode.isNull() ? responseNode : payload;
                }
                if (responseNode != null && !responseNode.isNull()) {
                    fallbackPayload = responseNode;
                    continue;
                }
            }
            fallbackPayload = payload;
        }
        if (fallbackPayload != null) {
            return fallbackPayload;
        }
        throw new IOException("Upstream returned SSE payload without JSON data");
    }

    private String buildFallbackStreamData(ProxyRequest request, String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return rawPayload;
        }
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            if (request != null && request.getProtocol() == ProxyProtocol.RESPONSES) {
                return buildResponsesFallbackData(payload);
            }
            if (request != null && request.getProtocol() == ProxyProtocol.CHAT_COMPLETIONS) {
                return buildChatCompletionsFallbackData(request, payload);
            }
            return rawPayload;
        } catch (Exception e) {
            return rawPayload;
        }
    }

    private String buildResponsesFallbackData(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return "{}";
        }
        String type = payload.path("type").asText();
        if (type != null && !type.isBlank()) {
            return writeJson(payload);
        }
        if (payload.has("response")) {
            return writeJson(payload);
        }
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("type", "response.completed");
        wrapper.set("response", payload);
        return writeJson(wrapper);
    }

    private String buildChatCompletionsFallbackData(ProxyRequest request, JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return "{}";
        }
        String object = payload.path("object").asText();
        if ("chat.completion.chunk".equals(object)) {
            return writeJson(payload);
        }
        if (!"chat.completion".equals(object)) {
            return writeJson(payload);
        }

        ObjectNode chunk = objectMapper.createObjectNode();
        chunk.put("id", textOrDefault(payload, "id", "chatcmpl-fallback"));
        chunk.put("object", "chat.completion.chunk");
        long created = payload.path("created").isNumber() ? payload.path("created").asLong() : System.currentTimeMillis() / 1000;
        chunk.put("created", created);

        String model = textOrDefault(payload, "model", request == null ? null : request.getModel());
        if (model != null && !model.isBlank()) {
            chunk.put("model", model);
        }

        ArrayNode choices = chunk.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.put("index", firstChoiceIndex(payload));
        ObjectNode delta = choice.putObject("delta");

        JsonNode firstChoice = firstChoice(payload);
        String role = "assistant";
        String content = null;
        String finishReason = "stop";
        if (firstChoice != null) {
            JsonNode messageNode = firstChoice.path("message");
            if (messageNode.isObject()) {
                String parsedRole = messageNode.path("role").asText();
                if (parsedRole != null && !parsedRole.isBlank()) {
                    role = parsedRole;
                }
                JsonNode contentNode = messageNode.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    content = contentNode.asText();
                }
            }

            JsonNode deltaNode = firstChoice.path("delta");
            if ((content == null || content.isBlank()) && deltaNode.isObject()) {
                JsonNode contentNode = deltaNode.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    content = contentNode.asText();
                }
            }
            if (deltaNode.isObject()) {
                String parsedRole = deltaNode.path("role").asText();
                if (parsedRole != null && !parsedRole.isBlank()) {
                    role = parsedRole;
                }
            }

            String parsedFinishReason = firstChoice.path("finish_reason").asText();
            if (parsedFinishReason != null && !parsedFinishReason.isBlank()) {
                finishReason = parsedFinishReason;
            }
        }
        delta.put("role", role);
        if (content != null) {
            delta.put("content", content);
        }
        choice.put("finish_reason", finishReason);

        if (payload.has("usage")) {
            chunk.set("usage", payload.get("usage"));
        }
        return writeJson(chunk);
    }

    private JsonNode firstChoice(JsonNode payload) {
        JsonNode choices = payload.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            return null;
        }
        return choices.get(0);
    }

    private int firstChoiceIndex(JsonNode payload) {
        JsonNode first = firstChoice(payload);
        if (first != null && first.path("index").canConvertToInt()) {
            return first.path("index").asInt();
        }
        return 0;
    }

    private String textOrDefault(JsonNode payload, String fieldName, String fallback) {
        if (payload == null || fieldName == null) {
            return fallback;
        }
        String value = payload.path(fieldName).asText();
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String writeJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return payload == null ? "{}" : payload.toString();
        }
    }

    private String buildFallbackErrorJson(String message) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode error = root.putObject("error");
        error.put("message", message == null || message.isBlank() ? "stream fallback error" : message);
        error.put("type", "server_error");
        error.putNull("param");
        error.put("code", "stream_fallback_error");
        return writeJson(root);
    }

    private String buildResponsesFallbackErrorEventJson(String message) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "error");
        ObjectNode error = root.putObject("error");
        error.put("message", message == null || message.isBlank() ? "stream fallback error" : message);
        error.put("type", "server_error");
        error.putNull("param");
        error.put("code", "stream_fallback_error");
        return writeJson(root);
    }

    private void emitFallbackErrorEvent(ProxyRequest request,
                                        Consumer<StreamEvent> onEvent,
                                        String message) {
        ProxyProtocol protocol = request == null ? null : request.getProtocol();
        if (protocol == ProxyProtocol.RESPONSES) {
            onEvent.accept(StreamEvent.builder()
                    .event("error")
                    .dataLine(buildResponsesFallbackErrorEventJson(message))
                    .build());
            return;
        }
        String errorJson;
        try {
            errorJson = OpenAIErrorResponse.internalError(message).toJson();
        } catch (Exception ex) {
            errorJson = buildFallbackErrorJson(message);
        }
        onEvent.accept(StreamEvent.builder().dataLine(errorJson).build());
        onEvent.accept(StreamEvent.builder().dataLine("[DONE]").build());
    }

    private String extractResponsesEventName(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path("type").asText();
            if (type == null || type.isBlank()) {
                return null;
            }
            return type;
        } catch (Exception e) {
            return null;
        }
    }
}
