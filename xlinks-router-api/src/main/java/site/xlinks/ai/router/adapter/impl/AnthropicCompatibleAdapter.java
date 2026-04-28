package site.xlinks.ai.router.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapter;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.service.ClientAbortException;
import site.xlinks.ai.router.service.StreamFirstResponseTimeoutException;
import site.xlinks.ai.router.service.UpstreamTransportException;
import site.xlinks.ai.router.service.UpstreamProviderException;
import site.xlinks.ai.router.service.UpstreamTimeoutException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Adapter for Anthropic-compatible /messages protocol.
 */
@Slf4j
@Component
public class AnthropicCompatibleAdapter extends AbstractSseHttpAdapter implements ProviderProtocolAdapter {

    private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
    private static final String HEADER_ANTHROPIC_BETA = "anthropic-beta";
    private static final String HEADER_X_API_KEY = "x-api-key";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String DEFAULT_ANTHROPIC_VERSION = "2023-06-01";
    private static final String DEFAULT_FORWARD_USER_AGENT =
            "codex-tui/0.125.0 (Mac OS 26.3.1; arm64) zed/0.231.2_stable.221.cc335b70f85a17974a4c61f852dbebff8c4b1db8 (codex-tui; 0.125.0)";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${xlinks.router.forward.user-agent:" + DEFAULT_FORWARD_USER_AGENT + "}")
    private String forwardUserAgent = DEFAULT_FORWARD_USER_AGENT;

    public AnthropicCompatibleAdapter(OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    @Override
    public boolean supports(ProxyProtocol protocol) {
        return protocol == ProxyProtocol.ANTHROPIC_MESSAGES;
    }

    @Override
    public JsonNode forwardDirect(ProxyRequest request, ProviderInvokeContext context) {
        try {
            Request httpRequest = buildRequest(request, context);
            Call call = createScopedClient(context, false).newCall(httpRequest);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw buildProviderFailure(response, context);
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }
                String responseJson = body.string();
                log.debug("Anthropic upstream response: {}", responseJson);
                return parseJsonResponseBody(responseJson, response.header("Content-Type", ""), httpRequest.url().toString());
            }
        } catch (InterruptedIOException e) {
            throw new UpstreamTimeoutException("Upstream request timed out", e);
        } catch (IOException e) {
            log.error("Error calling anthropic provider API", e);
            throw new RuntimeException("Failed to call provider API: " + e.getMessage(), e);
        }
    }

    @Override
    public void forwardStream(ProxyRequest request,
                              ProviderInvokeContext context,
                              Consumer<StreamEvent> onEvent) {
        forwardStream(request, context, onEvent, new AtomicBoolean(false));
    }

    @Override
    public void forwardStream(ProxyRequest request,
                              ProviderInvokeContext context,
                              Consumer<StreamEvent> onEvent,
                              AtomicBoolean cancelled) {
        if (cancelled != null && cancelled.get()) {
            throw new ClientAbortException("Stream cancelled before upstream call execution");
        }
        try {
            Request httpRequest = buildRequest(request, context);
            Call call = createScopedClient(context, true).newCall(httpRequest);
            Thread cancellationWatcher = startCancellationWatcher(call, cancelled);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw buildProviderFailure(response, context);
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Empty response from provider");
                }

                String contentType = response.header("Content-Type", "");
                if (isEventStream(contentType)) {
                    StreamReadResult readResult = readSseFrames(body, onEvent);
                    if (readResult.emittedAnyEvent()) {
                        return;
                    }
                    throw new IOException("Upstream provider returned an empty SSE stream");
                }

                String responseBody = body.string();
                if (looksLikeSse(responseBody)) {
                    emitParsedEvents(responseBody, onEvent);
                    return;
                }
                throw new IOException("Upstream provider did not return SSE for stream request. contentType="
                        + contentType + ", bodyPreview=" + abbreviate(responseBody, 600));
            } finally {
                stopCancellationWatcher(cancellationWatcher);
            }
        } catch (InterruptedIOException e) {
            throw new StreamFirstResponseTimeoutException("Stream first response timeout", e);
        } catch (IOException e) {
            if (cancelled != null && cancelled.get()) {
                throw new ClientAbortException("Downstream client disconnected while reading upstream stream", e);
            }
            log.error("Error calling anthropic provider API", e);
            throw new UpstreamTransportException("Failed to call provider API: " + e.getMessage(), e);
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
                .addHeader("User-Agent", forwardUserAgent)
                .addHeader(HEADER_X_API_KEY, context.getProviderToken())
                .addHeader(HEADER_AUTHORIZATION, "Bearer " + context.getProviderToken())
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

    private RuntimeException buildProviderFailure(Response response, ProviderInvokeContext context) throws IOException {
        String responseBody = response.body() == null ? "" : response.body().string();
        log.error("Anthropic API call failed: requestId={}, providerId={}, providerTokenId={}, baseUrl={}, statusCode={}, message={}, body={}",
                context == null ? null : context.getRequestId(),
                context == null ? null : context.getProviderId(),
                context == null ? null : context.getProviderTokenId(),
                context == null ? null : context.getBaseUrl(),
                response.code(),
                response.message(),
                responseBody);
        return new UpstreamProviderException(
                response.code(),
                "Provider API call failed: " + response.code(),
                responseBody
        );
    }

    JsonNode parseJsonResponseBody(String responseBody, String contentType, String requestUrl) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("Empty response from provider");
        }
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase();
        if (normalizedContentType.contains("text/html") || looksLikeHtml(responseBody)) {
            throw new IOException("Upstream provider returned HTML instead of JSON. Check provider baseUrl/path: " + requestUrl);
        }
        return objectMapper.readTree(responseBody);
    }

    private void emitParsedEvents(String responseBody, Consumer<StreamEvent> onEvent) throws IOException {
        List<StreamEvent> events = parseEvents(responseBody);
        if (events.isEmpty()) {
            throw new IOException("Upstream provider returned malformed SSE payload");
        }
        for (StreamEvent event : events) {
            onEvent.accept(event);
        }
    }
}
