package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import site.xlinks.ai.router.dto.OpenAIProxyRequest;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for providers exposing OpenAI-compatible HTTP APIs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleAdapter implements OpenAIProviderAdapter {

    private static final String PROVIDER_TYPE = "openai-compatible";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String providerType) {
        return PROVIDER_TYPE.equalsIgnoreCase(providerType)
                || "openai".equalsIgnoreCase(providerType);
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

                String responseJson = body.string();
                log.debug("Upstream {} response: {}", request.getProtocol(), responseJson);
                return objectMapper.readTree(responseJson);
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

                BufferedSource source = body.source();
                List<String> frameLines = new ArrayList<>();
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) {
                        break;
                    }
                    if (line.isEmpty()) {
                        emitEvent(frameLines, onEvent);
                        if (isDoneFrame(frameLines)) {
                            break;
                        }
                        frameLines.clear();
                        continue;
                    }
                    frameLines.add(line);
                }

                if (!frameLines.isEmpty()) {
                    emitEvent(frameLines, onEvent);
                }
            }
        } catch (IOException e) {
            log.error("Error calling provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    private Request buildRequest(OpenAIProxyRequest request, ProviderInvokeContext context) {
        String url = context.getBaseUrl() + request.getProtocol().getProviderPath();
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + context.getProviderToken())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(request.getRequestBody(), JSON))
                .build();
    }

    private RuntimeException buildProviderFailure(Response response) throws IOException {
        String responseBody = response.body() == null ? "" : response.body().string();
        log.error("Provider API call failed: {} - {}, body={}", response.code(), response.message(), responseBody);
        return new RuntimeException("Provider API call failed: " + response.code());
    }

    private void emitEvent(List<String> frameLines, Consumer<OpenAIStreamEvent> onEvent) {
        OpenAIStreamEvent event = parseEvent(frameLines);
        if (event != null) {
            onEvent.accept(event);
        }
    }

    private boolean isDoneFrame(List<String> frameLines) {
        OpenAIStreamEvent event = parseEvent(frameLines);
        return event != null && event.isDoneSignal();
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
}
