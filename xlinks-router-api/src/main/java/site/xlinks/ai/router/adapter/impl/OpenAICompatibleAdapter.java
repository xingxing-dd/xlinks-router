package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.adapter.OpenAIProviderAdapter;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.OpenAIProtocol;
import site.xlinks.ai.router.dto.OpenAIProxyRequest;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;
import site.xlinks.ai.router.openai.error.OpenAIErrorResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for providers exposing OpenAI-compatible HTTP APIs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleAdapter implements OpenAIProviderAdapter {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String EVENT_STREAM_CONTENT_TYPE = "text/event-stream";
    private static final int MAX_FALLBACK_STREAM_PAYLOAD_CHARS = 200_000;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OpenAIProtocol protocol) {
        return protocol != null;
    }

    @Override
    public JsonNode forward(OpenAIProxyRequest request, ProviderInvokeContext context) {
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

                String contentType = response.header("Content-Type", "");
                String responseJson = body.string();
                log.debug("Upstream {} response: {}", request.getProtocol(), responseJson);
                return parseResponseBody(request, responseJson, contentType);
            }
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    @Override
    public void forwardStream(OpenAIProxyRequest request,
                              ProviderInvokeContext context,
                              Consumer<OpenAIStreamEvent> onEvent) {
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

                String contentType = response.header("Content-Type", "");
                BufferedSource source = body.source();
                List<String> frameLines = new ArrayList<>();
                List<String> rawLines = new ArrayList<>();
                boolean emittedAnyEvent = false;
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) {
                        break;
                    }
                    rawLines.add(line);
                    if (line.isEmpty()) {
                        OpenAIStreamEvent event = parseEvent(frameLines);
                        if (event != null) {
                            onEvent.accept(event);
                            emittedAnyEvent = true;
                        }
                        if (event != null && event.isDoneSignal()) {
                            break;
                        }
                        frameLines.clear();
                        continue;
                    }
                    frameLines.add(line);
                }

                if (!frameLines.isEmpty()) {
                    OpenAIStreamEvent event = parseEvent(frameLines);
                    if (event != null) {
                        onEvent.accept(event);
                        emittedAnyEvent = true;
                    }
                }

                if (!emittedAnyEvent) {
                    String rawPayload = String.join("\n", rawLines).trim();
                    if (!rawPayload.isEmpty()) {
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
                        OpenAIProtocol protocol = request == null ? null : request.getProtocol();
                        if (protocol == OpenAIProtocol.RESPONSES) {
                            String eventName = extractResponsesEventName(fallbackData);
                            OpenAIStreamEvent.OpenAIStreamEventBuilder builder = OpenAIStreamEvent.builder()
                                    .dataLine(fallbackData);
                            if (eventName != null && !eventName.isBlank()) {
                                builder.event(eventName);
                            }
                            onEvent.accept(builder.build());
                            return;
                        }
                        onEvent.accept(OpenAIStreamEvent.builder().dataLine(fallbackData).build());
                        onEvent.accept(OpenAIStreamEvent.builder().dataLine("[DONE]").build());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    Request buildRequest(OpenAIProxyRequest request, ProviderInvokeContext context) {
        String url = context.getBaseUrl() + request.getProtocol().getProviderPath();
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + context.getProviderToken())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", request.isStream() ? EVENT_STREAM_CONTENT_TYPE : "application/json")
                .post(RequestBody.create(rewriteRequestBody(request, context), JSON))
                .build();
    }

    String rewriteRequestBody(OpenAIProxyRequest request, ProviderInvokeContext context) {
        JsonNode payload = request == null ? null : request.getPayload();
        if (payload instanceof ObjectNode payloadObject) {
            try {
                ObjectNode objectNode = payloadObject.deepCopy();
                if (context.getProviderModel() != null && !context.getProviderModel().isBlank()) {
                    objectNode.put("model", context.getProviderModel());
                }
                objectNode.put("stream", request != null && request.isStream());
                return objectMapper.writeValueAsString(objectNode);
            } catch (Exception e) {
                log.warn("Failed to serialize rewritten payload, fallback to raw body: {}", e.getMessage());
            }
        }

        String requestBody = request == null ? null : request.getRequestBody();
        if (requestBody == null || requestBody.isBlank()) {
            return requestBody;
        }
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            if (!(root instanceof ObjectNode objectNode)) {
                return requestBody;
            }
            if (context.getProviderModel() != null && !context.getProviderModel().isBlank()) {
                objectNode.put("model", context.getProviderModel());
            }
            objectNode.put("stream", request != null && request.isStream());
            return objectMapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            log.warn("Failed to rewrite request model for provider, fallback to original body: {}", e.getMessage());
            return requestBody;
        }
    }

    private RuntimeException buildProviderFailure(Response response) throws IOException {
        String responseBody = response.body() == null ? "" : response.body().string();
        log.error("Provider API call failed: {} - {}, body={}", response.code(), response.message(), responseBody);
        return new RuntimeException("Provider API call failed: " + response.code());
    }

    JsonNode parseResponseBody(OpenAIProxyRequest request,
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

    private OpenAIStreamEvent parseEvent(List<String> frameLines) {
        if (frameLines == null || frameLines.isEmpty()) {
            return null;
        }

        OpenAIStreamEvent.OpenAIStreamEventBuilder builder = OpenAIStreamEvent.builder();
        boolean hasAnyField = false;
        for (String line : frameLines) {
            if (line == null) {
                continue;
            }
            if (line.startsWith(":")) {
                builder.comment(line.substring(1));
                hasAnyField = true;
                continue;
            }

            int separatorIndex = line.indexOf(':');
            String fieldName;
            String fieldValue;
            if (separatorIndex < 0) {
                fieldName = line;
                fieldValue = "";
            } else {
                fieldName = line.substring(0, separatorIndex);
                fieldValue = line.substring(separatorIndex + 1);
                if (fieldValue.startsWith(" ")) {
                    fieldValue = fieldValue.substring(1);
                }
            }

            switch (fieldName) {
                case "event" -> {
                    builder.event(fieldValue);
                    hasAnyField = true;
                }
                case "data" -> {
                    builder.dataLine(fieldValue);
                    hasAnyField = true;
                }
                case "id" -> {
                    builder.id(fieldValue);
                    hasAnyField = true;
                }
                case "retry" -> {
                    try {
                        builder.retry(Long.parseLong(fieldValue));
                        hasAnyField = true;
                    } catch (NumberFormatException e) {
                        log.debug("Ignoring invalid SSE retry value: {}", fieldValue);
                    }
                }
                default -> {
                    // Ignore unsupported SSE fields.
                }
            }
        }
        return hasAnyField ? builder.build() : null;
    }

    private boolean isEventStream(String contentType) {
        return contentType != null && contentType.toLowerCase().contains(EVENT_STREAM_CONTENT_TYPE);
    }

    private boolean looksLikeSse(String responseBody) {
        try (StringReader reader = new StringReader(responseBody)) {
            StringBuilder firstLine = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                if (ch == '\n' || ch == '\r') {
                    break;
                }
                firstLine.append((char) ch);
            }
            String line = firstLine.toString().trim();
            return line.startsWith("event:") || line.startsWith("data:") || line.startsWith(":");
        } catch (IOException e) {
            return false;
        }
    }

    private JsonNode parseSseResponseBody(OpenAIProxyRequest request, String responseBody) throws IOException {
        JsonNode fallbackPayload = null;
        for (OpenAIStreamEvent event : parseEvents(responseBody)) {
            if (!event.hasData()) {
                continue;
            }
            String data = event.joinedData();
            if (data == null || data.isBlank() || "[DONE]".equals(data)) {
                continue;
            }
            JsonNode payload = objectMapper.readTree(data);
            if (request != null && request.getProtocol() == OpenAIProtocol.RESPONSES) {
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

    private List<OpenAIStreamEvent> parseEvents(String responseBody) {
        List<OpenAIStreamEvent> events = new ArrayList<>();
        List<String> frameLines = new ArrayList<>();
        String[] lines = responseBody.split("\\R", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                OpenAIStreamEvent event = parseEvent(frameLines);
                if (event != null) {
                    events.add(event);
                }
                frameLines.clear();
                continue;
            }
            frameLines.add(line);
        }
        if (!frameLines.isEmpty()) {
            OpenAIStreamEvent event = parseEvent(frameLines);
            if (event != null) {
                events.add(event);
            }
        }
        return events.isEmpty() ? Collections.emptyList() : events;
    }

    private String abbreviate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (maxLen <= 3 || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen - 3) + "...";
    }

    private String buildFallbackStreamData(OpenAIProxyRequest request, String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return rawPayload;
        }
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            if (request != null && request.getProtocol() == OpenAIProtocol.RESPONSES) {
                return buildResponsesFallbackData(payload);
            }
            if (request != null && request.getProtocol() == OpenAIProtocol.CHAT_COMPLETIONS) {
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

    private String buildChatCompletionsFallbackData(OpenAIProxyRequest request, JsonNode payload) {
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

    private void emitFallbackErrorEvent(OpenAIProxyRequest request,
                                        Consumer<OpenAIStreamEvent> onEvent,
                                        String message) {
        OpenAIProtocol protocol = request == null ? null : request.getProtocol();
        if (protocol == OpenAIProtocol.RESPONSES) {
            onEvent.accept(OpenAIStreamEvent.builder()
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
        onEvent.accept(OpenAIStreamEvent.builder().dataLine(errorJson).build());
        onEvent.accept(OpenAIStreamEvent.builder().dataLine("[DONE]").build());
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
