package site.xlinks.ai.router.distributed.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingPreparation;
import site.xlinks.ai.router.distributed.infrastructure.http.model.ProviderInvokeContext;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOkHttpProviderHttpAdapter implements ProviderHttpAdapter {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    protected final OkHttpClient okHttpClient;
    protected final ObjectMapper objectMapper;

    @Override
    public Object forward(ForwardingPreparation preparation) {
        ProviderInvokeContext context = buildContext(preparation);
        if (preparation.getRequest().isStream()) {
            return forwardStream(preparation, context);
        }
        return forwardDirect(preparation, context);
    }

    protected abstract Request buildRequest(ForwardingPreparation preparation, ProviderInvokeContext context);

    protected ProviderInvokeContext buildContext(ForwardingPreparation preparation) {
        return ProviderInvokeContext.builder()
                .providerId(preparation.getRoutingDecision().getProvider().getId())
                .providerTokenId(preparation.getRoutingDecision().getProviderToken().getId())
                .baseUrl(normalizeBaseUrl(preparation.getRoutingDecision().getProvider().getBaseUrl()))
                .providerToken(preparation.getRoutingDecision().getProviderToken().getTokenValue())
                .providerModelCode(preparation.getRoutingDecision().getProviderModel().getProviderModelCode())
                .requestTimeoutMs(preparation.getRoutingDecision().getProvider().getRequestTimeoutMs())
                .streamFirstResponseTimeoutMs(preparation.getRoutingDecision().getProvider().getStreamFirstResponseTimeoutMs())
                .build();
    }

    protected ResponseEntity<String> forwardDirect(ForwardingPreparation preparation, ProviderInvokeContext context) {
        Request request = buildRequest(preparation, context);
        Call call = createScopedClient(context, false).newCall(request);
        try (Response response = call.execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            return ResponseEntity.status(response.code())
                    .headers(buildResponseHeaders(response))
                    .body(responseBody);
        } catch (Exception ex) {
            log.error("Provider direct forwarding failed. providerId={}, providerTokenId={}, protocol={}",
                    context.getProviderId(), context.getProviderTokenId(), preparation.getRequest().getProtocol().getCode(), ex);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Provider direct forwarding failed");
        }
    }

    protected ResponseEntity<StreamingResponseBody> forwardStream(ForwardingPreparation preparation, ProviderInvokeContext context) {
        Request request = buildRequest(preparation, context);
        StreamingResponseBody body = outputStream -> executeStreamForward(request, context, outputStream);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");
        headers.add("X-Accel-Buffering", "no");
        return ResponseEntity.ok().headers(headers).body(body);
    }

    protected void executeStreamForward(Request request, ProviderInvokeContext context, OutputStream outputStream) {
        Call call = createScopedClient(context, true).newCall(request);
        try (Response response = call.execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return;
            }
            try (InputStream inputStream = responseBody.byteStream()) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }
        } catch (Exception ex) {
            log.error("Provider stream forwarding failed. providerId={}, providerTokenId={}",
                    context.getProviderId(), context.getProviderTokenId(), ex);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Provider stream forwarding failed");
        }
    }

    protected OkHttpClient createScopedClient(ProviderInvokeContext context, boolean stream) {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        long timeoutMs = resolveTimeoutMs(context, stream);
        if (stream) {
            builder.callTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS);
            return builder.build();
        }
        return builder.callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    protected String rewriteRequestBody(ForwardRequest request, ProviderInvokeContext context) {
        JsonNode payload = request.getPayload();
        if (payload instanceof ObjectNode objectNode) {
            try {
                ObjectNode copy = objectNode.deepCopy();
                if (context.getProviderModelCode() != null && !context.getProviderModelCode().isBlank()) {
                    copy.put("model", context.getProviderModelCode());
                }
                if (request.getStream() != null) {
                    copy.put("stream", request.isStream());
                }
                return objectMapper.writeValueAsString(copy);
            } catch (Exception ex) {
                log.warn("Rewrite request body from parsed payload failed, fallback to raw body", ex);
            }
        }
        try {
            JsonNode root = objectMapper.readTree(request.getRequestBody());
            if (root instanceof ObjectNode objectNode) {
                if (context.getProviderModelCode() != null && !context.getProviderModelCode().isBlank()) {
                    objectNode.put("model", context.getProviderModelCode());
                }
                if (request.getStream() != null) {
                    objectNode.put("stream", request.isStream());
                }
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception ex) {
            log.warn("Rewrite request body from raw body failed, fallback to original request body", ex);
        }
        return request.getRequestBody();
    }

    protected HttpHeaders buildResponseHeaders(Response response) {
        HttpHeaders headers = new HttpHeaders();
        String contentType = response.header("Content-Type");
        if (contentType != null && !contentType.isBlank()) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        } else {
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    protected RequestBody buildJsonRequestBody(ForwardRequest request, ProviderInvokeContext context) {
        return RequestBody.create(rewriteRequestBody(request, context), JSON);
    }

    protected String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.PROVIDER_UNAVAILABLE, "Provider baseUrl is missing");
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    protected long resolveTimeoutMs(ProviderInvokeContext context, boolean stream) {
        Integer configured = stream ? context.getStreamFirstResponseTimeoutMs() : context.getRequestTimeoutMs();
        if (configured == null || configured <= 0) {
            return Duration.ofSeconds(20).toMillis();
        }
        return configured.longValue();
    }
}
