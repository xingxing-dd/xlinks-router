package site.xlinks.ai.router.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.app.forwarding.model.ForwardingUsageContext;
import site.xlinks.ai.router.infrastructure.http.exception.UpstreamRetryableException;
import site.xlinks.ai.router.infrastructure.http.exception.UpstreamTimeoutException;
import site.xlinks.ai.router.infrastructure.http.model.ProviderInvokeContext;
import site.xlinks.ai.router.infrastructure.http.model.ProviderStreamHandle;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public abstract class AbstractOkHttpProviderHttpAdapter implements ProviderHttpAdapter {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    protected final OkHttpClient okHttpClient;
    protected final ObjectMapper objectMapper;

    @Override
    public Object forward(ForwardingDecision decision) {
        ProviderInvokeContext context = buildContext(decision);
        if (decision.getRequest().isStream()) {
            return forwardStream(decision, context);
        }
        return forwardDirect(decision, context);
    }

    protected abstract Request buildRequest(ForwardingDecision decision, ProviderInvokeContext context);

    protected ProviderInvokeContext buildContext(ForwardingDecision decision) {
        ForwardingUsageContext usageContext = decision.getUsageContext();
        return ProviderInvokeContext.builder()
                .requestId(usageContext == null ? decision.getRequestId() : usageContext.getRequestId())
                .accountId(usageContext == null ? null : usageContext.getAccountId())
                .customerTokenId(usageContext == null ? null : usageContext.getCustomerTokenId())
                .customerTokenValue(usageContext == null ? null : usageContext.getCustomerTokenValue())
                .planId(usageContext == null ? null : usageContext.getPlanId())
                .providerId(decision.getSelectedRoute().getProvider().getId())
                .providerTokenId(decision.getSelectedRoute().getProviderToken().getId())
                .providerTokenName(usageContext == null ? null : usageContext.getProviderTokenName())
                .providerCode(usageContext == null ? null : usageContext.getProviderCode())
                .providerName(usageContext == null ? null : usageContext.getProviderName())
                .baseUrl(normalizeBaseUrl(decision.getSelectedRoute().getProvider().getBaseUrl()))
                .providerToken(decision.getSelectedRoute().getProviderToken().getTokenValue())
                .providerModelCode(decision.getSelectedRoute().getProviderModel().getProviderModelCode())
                .endpointCode(usageContext == null ? null : usageContext.getEndpointCode())
                .modelId(usageContext == null ? null : usageContext.getModelId())
                .modelCode(usageContext == null ? null : usageContext.getModelCode())
                .modelName(usageContext == null ? null : usageContext.getModelName())
                .modelProvider(usageContext == null ? null : usageContext.getModelProvider())
                .inputPrice(usageContext == null ? null : usageContext.getInputPrice())
                .cacheHitPrice(usageContext == null ? null : usageContext.getCacheHitPrice())
                .outputPrice(usageContext == null ? null : usageContext.getOutputPrice())
                .multiplier(usageContext == null ? null : usageContext.getMultiplier())
                .requestTimeoutMs(resolveRequestTimeoutMs(decision))
                .streamFirstResponseTimeoutMs(resolveStreamFirstResponseTimeoutMs(decision))
                .streamIdleTimeoutMs(resolveStreamIdleTimeoutMs(decision))
                .build();
    }

    protected ResponseEntity<String> forwardDirect(ForwardingDecision decision, ProviderInvokeContext context) {
        Request request = buildRequest(decision, context);
        RequestChainLogCollector.record(
                RequestChainLogType.UPSTREAM_DIRECT_REQUEST,
                describeProvider(context),
                describeProviderToken(context),
                maskTokenValue(context.getProviderToken()),
                request.url()
        );
        Call call = createScopedClient(context, false).newCall(request);
        try (Response response = call.execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            ResponseEntity<String> responseEntity = ResponseEntity.status(response.code())
                    .headers(buildResponseHeaders(response))
                    .body(responseBody);
            RequestChainLogCollector.record(RequestChainLogType.UPSTREAM_DIRECT_RESPONSE, response.code());
            if (shouldRetryStatus(response.code())) {
                throw new UpstreamRetryableException("上游返回可重试状态码", responseEntity);
            }
            return responseEntity;
        } catch (InterruptedIOException ex) {
            RequestChainLogCollector.record(RequestChainLogType.UPSTREAM_DIRECT_TIMEOUT, describeProvider(context));
            throw new UpstreamTimeoutException("上游请求超时", ex);
        } catch (UpstreamRetryableException ex) {
            throw ex;
        } catch (Exception ex) {
            RequestChainLogCollector.record(
                    RequestChainLogType.UPSTREAM_DIRECT_ERROR,
                    describeProvider(context),
                    valueOrDash(ex.getMessage())
            );
            throw new UpstreamRetryableException("上游直连转发失败", ex);
        }
    }

    protected Object forwardStream(ForwardingDecision decision, ProviderInvokeContext context) {
        Request request = buildRequest(decision, context);
        RequestChainLogCollector.record(
                RequestChainLogType.UPSTREAM_STREAM_REQUEST,
                describeProvider(context),
                describeProviderToken(context),
                maskTokenValue(context.getProviderToken()),
                request.url()
        );
        Call call = createScopedClient(context, true).newCall(request);
        try {
            Response response = call.execute();
            ResponseBody responseBody = response.body();
            RequestChainLogCollector.record(RequestChainLogType.UPSTREAM_STREAM_RESPONSE, response.code());
            if (shouldRetryStatus(response.code())) {
                String responseContent = responseBody == null ? "" : responseBody.string();
                try (response) {
                    throw new UpstreamRetryableException(
                            "上游流式响应返回可重试状态码",
                            ResponseEntity.status(response.code()).headers(buildResponseHeaders(response)).body(responseContent)
                    );
                }
            }
            if (!response.isSuccessful()) {
                String responseContent = responseBody == null ? "" : responseBody.string();
                try (response) {
                    return ResponseEntity.status(response.code())
                            .headers(buildResponseHeaders(response))
                            .body(responseContent);
                }
            }
            if (responseBody == null) {
                try (response) {
                    return ResponseEntity.status(response.code())
                            .headers(buildStreamHeaders(response))
                            .body("");
                }
            }
            applyStreamIdleTimeout(responseBody, context);
            InputStream inputStream = responseBody.byteStream();
            ProviderStreamHandle streamHandle = new ProviderStreamHandle(response, responseBody, inputStream);
            return ResponseEntity.status(response.code()).headers(buildStreamHeaders(response)).body(streamHandle);
        } catch (InterruptedIOException ex) {
            RequestChainLogCollector.record(RequestChainLogType.UPSTREAM_STREAM_TIMEOUT, describeProvider(context));
            throw new UpstreamTimeoutException("上游请求超时", ex);
        } catch (UpstreamRetryableException ex) {
            throw ex;
        } catch (Exception ex) {
            RequestChainLogCollector.record(
                    RequestChainLogType.UPSTREAM_STREAM_ERROR,
                    describeProvider(context),
                    valueOrDash(ex.getMessage())
            );
            throw new UpstreamRetryableException("上游流式转发失败", ex);
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
                applyProviderOverrides(copy, request, context);
                return objectMapper.writeValueAsString(copy);
            } catch (Exception ex) {
                RequestChainLogCollector.record(RequestChainLogType.REQUEST_REWRITE_FALLBACK_FROM_PAYLOAD);
            }
        }
        try {
            JsonNode root = objectMapper.readTree(request.getRequestBody());
            if (root instanceof ObjectNode objectNode) {
                applyProviderOverrides(objectNode, request, context);
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception ex) {
            RequestChainLogCollector.record(RequestChainLogType.REQUEST_REWRITE_FALLBACK_FROM_RAW);
        }
        return request.getRequestBody();
    }

    protected HttpHeaders buildResponseHeaders(Response response) {
        HttpHeaders headers = new HttpHeaders();
        String contentType = response.header("Content-Type");
        if (hasText(contentType)) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        } else {
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    protected HttpHeaders buildStreamHeaders(Response response) {
        HttpHeaders headers = new HttpHeaders();
        String contentType = response.header("Content-Type");
        if (hasText(contentType)) {
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        } else {
            headers.add(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
        }
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");
        headers.add("X-Accel-Buffering", "no");
        return headers;
    }

    protected RequestBody buildJsonRequestBody(ForwardRequest request, ProviderInvokeContext context) {
        return RequestBody.create(rewriteRequestBody(request, context), JSON);
    }

    protected String buildRequestUrl(String baseUrl, String providerPath) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        if (!hasText(providerPath)) {
            return normalizedBaseUrl;
        }
        String normalizedProviderPath = providerPath.startsWith("/") ? providerPath : "/" + providerPath;
        String overlap = longestPathOverlap(normalizedBaseUrl, normalizedProviderPath);
        if (!overlap.isEmpty()) {
            return normalizedBaseUrl + normalizedProviderPath.substring(overlap.length());
        }
        return normalizedBaseUrl + normalizedProviderPath;
    }

    protected String normalizeBaseUrl(String baseUrl) {
        if (!hasText(baseUrl)) {
            throw new BusinessException(ErrorCode.PROVIDER_UNAVAILABLE, "服务商基础地址缺失");
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

    protected boolean shouldRetryStatus(int statusCode) {
        return statusCode == 408
                || statusCode == 409
                || statusCode == 425
                || statusCode == 429
                || statusCode >= 500;
    }

    private Integer resolveRequestTimeoutMs(ForwardingDecision decision) {
        if (decision.getProviderPermitLease() != null
                && decision.getProviderPermitLease().runtimePolicy() != null) {
            return decision.getProviderPermitLease().runtimePolicy().requestTimeoutMs();
        }
        return decision.getSelectedRoute().getProvider().getRequestTimeoutMs();
    }

    private Integer resolveStreamFirstResponseTimeoutMs(ForwardingDecision decision) {
        if (decision.getProviderPermitLease() != null
                && decision.getProviderPermitLease().runtimePolicy() != null) {
            return decision.getProviderPermitLease().runtimePolicy().streamFirstResponseTimeoutMs();
        }
        return decision.getSelectedRoute().getProvider().getStreamFirstResponseTimeoutMs();
    }

    private Integer resolveStreamIdleTimeoutMs(ForwardingDecision decision) {
        if (decision.getProviderPermitLease() != null
                && decision.getProviderPermitLease().runtimePolicy() != null) {
            return decision.getProviderPermitLease().runtimePolicy().streamIdleTimeoutMs();
        }
        return decision.getSelectedRoute().getProvider().getStreamIdleTimeoutMs();
    }

    protected void applyStreamIdleTimeout(ResponseBody responseBody, ProviderInvokeContext context) {
        if (responseBody == null || context == null) {
            return;
        }
        Integer streamIdleTimeoutMs = context.getStreamIdleTimeoutMs();
        if (streamIdleTimeoutMs == null || streamIdleTimeoutMs <= 0) {
            return;
        }
        responseBody.source().timeout().timeout(streamIdleTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private void applyProviderOverrides(ObjectNode objectNode,
                                        ForwardRequest request,
                                        ProviderInvokeContext context) {
        if (hasText(context.getProviderModelCode())) {
            objectNode.put("model", context.getProviderModelCode());
        }
        if (request.getStream() != null) {
            objectNode.put("stream", request.isStream());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String longestPathOverlap(String baseUrl, String providerPath) {
        int schemeSeparatorIndex = baseUrl.indexOf("://");
        int pathStartIndex = schemeSeparatorIndex >= 0
                ? baseUrl.indexOf('/', schemeSeparatorIndex + 3)
                : baseUrl.indexOf('/');
        String basePath = pathStartIndex >= 0 ? baseUrl.substring(pathStartIndex) : "";
        if (basePath.isEmpty()) {
            return "";
        }

        int maxLength = Math.min(basePath.length(), providerPath.length());
        for (int length = maxLength; length > 0; length--) {
            String suffix = basePath.substring(basePath.length() - length);
            if (!providerPath.startsWith(suffix)) {
                continue;
            }
            if (suffix.charAt(0) != '/') {
                continue;
            }
            if (length < providerPath.length() && providerPath.charAt(length) != '/') {
                continue;
            }
            return suffix;
        }
        return "";
    }

    private String describeProvider(ProviderInvokeContext context) {
        return valueOrDash(context.getProviderName())
                + "/" + valueOrDash(context.getProviderCode())
                + "(" + valueOrDash(context.getProviderId()) + ")";
    }

    private String describeProviderToken(ProviderInvokeContext context) {
        return valueOrDash(context.getProviderTokenName())
                + "(" + valueOrDash(context.getProviderTokenId()) + ")";
    }

    private String maskTokenValue(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return "-";
        }
        if (tokenValue.length() <= 4) {
            return "****";
        }
        if (tokenValue.length() <= 8) {
            return tokenValue.substring(0, 2) + "****" + tokenValue.substring(tokenValue.length() - 2);
        }
        return tokenValue.substring(0, 4) + "****" + tokenValue.substring(tokenValue.length() - 4);
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        return text.isBlank() ? "-" : text;
    }
}
