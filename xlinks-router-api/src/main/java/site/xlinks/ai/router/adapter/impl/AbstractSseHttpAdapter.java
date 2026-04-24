package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.service.StreamFirstResponseTimeoutException;
import site.xlinks.ai.router.service.StreamIdleTimeoutException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Shared HTTP/SSE utilities for protocol adapters.
 */
@Slf4j
public abstract class AbstractSseHttpAdapter {

    protected static final String EVENT_STREAM_CONTENT_TYPE = "text/event-stream";

    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected AbstractSseHttpAdapter(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    protected OkHttpClient createScopedClient(ProviderInvokeContext context, boolean stream) {
        OkHttpClient.Builder builder = httpClient.newBuilder();
        if (stream) {
            long readTimeoutMs = normalizeTimeout(streamTimeoutMs(context), 20_000L);
            builder.callTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
            return builder.build();
        }
        long requestTimeoutMs = normalizeTimeout(requestTimeoutMs(context), 20_000L);
        return builder
                .callTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    protected StreamReadResult readSseFrames(ResponseBody body,
                                             Consumer<StreamEvent> onEvent) throws IOException {
        if (body == null) {
            return new StreamReadResult(false, "");
        }
        BufferedSource source = body.source();
        List<String> frameLines = new ArrayList<>();
        List<String> rawLines = new ArrayList<>();
        boolean emittedAnyEvent = false;
        while (!source.exhausted()) {
            String line;
            try {
                line = source.readUtf8Line();
            } catch (InterruptedIOException e) {
                if (emittedAnyEvent) {
                    throw new StreamIdleTimeoutException("Stream idle timeout", e);
                }
                throw new StreamFirstResponseTimeoutException("Stream first response timeout", e);
            }
            if (line == null) {
                break;
            }
            rawLines.add(line);
            if (line.isEmpty()) {
                StreamEvent event = parseEvent(frameLines);
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
            StreamEvent event = parseEvent(frameLines);
            if (event != null) {
                onEvent.accept(event);
                emittedAnyEvent = true;
            }
        }
        return new StreamReadResult(emittedAnyEvent, String.join("\n", rawLines).trim());
    }

    protected String rewriteModelAndStream(ProxyRequest request, ProviderInvokeContext context) {
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

    protected StreamEvent parseEvent(List<String> frameLines) {
        if (frameLines == null || frameLines.isEmpty()) {
            return null;
        }

        StreamEvent.StreamEventBuilder builder = StreamEvent.builder();
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

    protected List<StreamEvent> parseEvents(String responseBody) {
        List<StreamEvent> events = new ArrayList<>();
        List<String> frameLines = new ArrayList<>();
        String[] lines = responseBody.split("\\R", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                StreamEvent event = parseEvent(frameLines);
                if (event != null) {
                    events.add(event);
                }
                frameLines.clear();
                continue;
            }
            frameLines.add(line);
        }
        if (!frameLines.isEmpty()) {
            StreamEvent event = parseEvent(frameLines);
            if (event != null) {
                events.add(event);
            }
        }
        return events.isEmpty() ? Collections.emptyList() : events;
    }

    protected boolean isEventStream(String contentType) {
        return contentType != null && contentType.toLowerCase().contains(EVENT_STREAM_CONTENT_TYPE);
    }

    protected boolean looksLikeSse(String responseBody) {
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

    protected boolean looksLikeHtml(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        String trimmed = responseBody.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase();
        return lower.startsWith("<!doctype html")
                || lower.startsWith("<html")
                || lower.startsWith("<head")
                || lower.startsWith("<body");
    }

    protected String abbreviate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (maxLen <= 3 || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen - 3) + "...";
    }

    private long requestTimeoutMs(ProviderInvokeContext context) {
        return context == null || context.getRequestTimeoutMs() == null ? 20_000L : context.getRequestTimeoutMs();
    }

    private long streamTimeoutMs(ProviderInvokeContext context) {
        return context == null || context.getStreamIdleTimeoutMs() == null ? 20_000L : context.getStreamIdleTimeoutMs();
    }

    private long normalizeTimeout(long value, long fallback) {
        if (value <= 0) {
            return fallback;
        }
        return value;
    }

    protected record StreamReadResult(boolean emittedAnyEvent, String rawPayload) {
    }
}
