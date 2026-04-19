package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapter;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapterFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.routing.ProxyRoutingPipeline;
import site.xlinks.ai.router.service.routing.ProxyErrors;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrates routing, provider invocation, and usage recording.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolProxyService {

    private final CustomerTokenAuthService customerTokenAuthService;
    private final ProviderTokenSelectService providerTokenSelectService;
    private final ProviderProtocolAdapterFactory adapterFactory;
    private final RouteCacheService routeCacheService;
    private final UsageRecordService usageRecordService;
    private final UsageEntitlementService usageEntitlementService;
    private final UsageExtractor usageExtractor;
    private final ProxyRoutingPipeline proxyRoutingPipeline;

    @Value("${xlinks.router.debug.log-upstream-responses-stream-payload:false}")
    private boolean logUpstreamResponsesStreamPayload;

    public JsonNode forwardDirect(String token, ProxyRequest request) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, request, requestId);
            ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
            JsonNode response = adapter.forwardDirect(request, context);
            UsageMetrics usageMetrics = usageExtractor.extract(response, context.getModelProvider());
            usageRecordService.recordAsync(context, usageMetrics, System.currentTimeMillis() - startAt, null, null);
            return response;
        } catch (BusinessException e) {
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (Exception e) {
            throw handleUnexpectedError("Proxy request failed", context, e, startAt);
        }
    }

    public void forwardStream(String token,
                              ProxyRequest request,
                              Consumer<StreamEvent> onEvent) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        boolean captureUpstreamStreamPayload = shouldCaptureUpstreamResponsesStreamPayload(request);
        StringBuilder upstreamStreamPayloadBuilder = captureUpstreamStreamPayload ? new StringBuilder() : null;
        try {
            context = buildContext(token, request, requestId);
            ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
            AtomicReference<UsageMetrics> usageMetricsRef = new AtomicReference<>();
            AtomicReference<Integer> responseMsRef = new AtomicReference<>();
            String modelProvider = context.getModelProvider();
            adapter.forwardStream(request, context, event -> {
                appendStreamEvent(upstreamStreamPayloadBuilder, event);
                if (isFirstResponseDataEvent(event)) {
                    long firstResponseMs = System.currentTimeMillis() - startAt;
                    int normalized = firstResponseMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(firstResponseMs, 0L);
                    responseMsRef.compareAndSet(null, normalized);
                }
                UsageMetrics usageMetrics = usageExtractor.extract(event, modelProvider);
                if (usageMetrics != null) {
                    usageMetricsRef.set(usageMetrics);
                }
                onEvent.accept(event);
            });
            usageRecordService.recordAsync(
                    context,
                    usageMetricsRef.get(),
                    System.currentTimeMillis() - startAt,
                    responseMsRef.get(),
                    null,
                    null
            );
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
        } catch (BusinessException e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (Exception e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            throw handleUnexpectedError("Proxy stream failed", context, e, startAt);
        }
    }

    public Object listModels(String token) {
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);
        long createdEpoch = System.currentTimeMillis() / 1000;
        List<Model> models = routeCacheService.listModels();
        List<Object> modelList = models.stream()
                .filter(model -> model.getModelCode() != null && !model.getModelCode().isBlank())
                .filter(model -> customerTokenAuthService.hasPermissionForModel(customerToken, model.getModelCode()))
                .map(model -> Map.of(
                        "id", model.getModelCode(),
                        "object", "model",
                        "created", createdEpoch,
                        "owned_by", "xlinks-router"
                ))
                .collect(Collectors.toList());

        return Map.of(
                "object", "list",
                "data", modelList
        );
    }

    private ProviderInvokeContext buildContext(String token,
                                               ProxyRequest request,
                                               String requestId) {
        RoutingBuildContext routingContext = proxyRoutingPipeline.resolve(token, request, requestId);
        CustomerToken customerToken = routingContext.getCustomerToken();
        UsageDecision usageDecision = routingContext.getUsageDecision();
        Model model = routingContext.getModel();
        Provider provider = routingContext.getProvider();
        ProviderModel providerModel = routingContext.getProviderModel();
        ProviderToken providerToken = routingContext.getProviderToken();
        String endpointCode = request.getProtocol().getCode();

        String upstreamModelCode = providerModel.getProviderModelCode();
        if (upstreamModelCode == null || upstreamModelCode.isBlank()) {
            upstreamModelCode = model.getModelCode();
        }
        return ProviderInvokeContext.builder()
                .requestId(requestId)
                .providerId(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .baseUrl(provider.getBaseUrl())
                .providerToken(providerToken.getTokenValue())
                .customerToken(token)
                .endpointCode(endpointCode)
                .modelId(model.getId())
                .modelCode(model.getModelCode())
                .modelName(model.getModelName())
                .inputPrice(model.getInputPrice())
                .cacheHitPrice(model.getCacheHitPrice())
                .outputPrice(model.getOutputPrice())
                .modelProvider(model.getModelProvider())
                .providerModel(upstreamModelCode)
                .planId(usageDecision.getPlanId())
                .customerTokenId(customerToken.getId())
                .accountId(customerToken.getAccountId())
                .customerModel(request.getModel())
                .build();
    }

    private ProviderProtocolAdapter resolveAdapter(ProxyProtocol protocol) {
        ProviderProtocolAdapter adapter = adapterFactory.getAdapter(protocol);
        if (adapter == null) {
            throw ProxyErrors.unsupportedProtocol(protocol);
        }
        return adapter;
    }

    private String buildRequestId(ProxyProtocol protocol) {
        return protocol.getRequestIdPrefix() + UUID.randomUUID().toString().substring(0, 8);
    }

    private void recordBusinessError(ProviderInvokeContext context, BusinessException e, long startAt) {
        recordError(context, resolveHttpStatus(e.getCode()), String.valueOf(e.getCode()), e.getMessage(), startAt);
    }

    private BusinessException handleUnexpectedError(String logMessage,
                                                    ProviderInvokeContext context,
                                                    Exception e,
                                                    long startAt) {
        log.error(logMessage, e);
        recordError(context, 500, ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), startAt);
        return ProxyErrors.requestProcessingFailed(e.getMessage());
    }

    private void recordError(ProviderInvokeContext context,
                             int responseStatus,
                             String errorCode,
                             String errorMessage,
                             long startAt) {
        if (context == null) {
            return;
        }
        usageRecordService.recordError(
                context,
                responseStatus,
                errorCode,
                errorMessage,
                System.currentTimeMillis() - startAt
        );
    }

    private int resolveHttpStatus(int businessCode) {
        if (businessCode == ErrorCode.UNAUTHORIZED.getCode()) {
            return 401;
        }
        if (businessCode == ErrorCode.FORBIDDEN.getCode()) {
            return 403;
        }
        if (businessCode >= 5000) {
            return 500;
        }
        return 400;
    }

    private boolean isFirstResponseDataEvent(StreamEvent event) {
        if (event == null || event.isDoneSignal()) {
            return false;
        }
        if (event.hasData()) {
            String data = event.joinedData();
            return data != null && !data.isBlank();
        }
        if (event.getEvent() != null && !event.getEvent().isBlank()) {
            return true;
        }
        return event.getComments() != null && !event.getComments().isEmpty();
    }

    private boolean shouldCaptureUpstreamResponsesStreamPayload(ProxyRequest request) {
        return logUpstreamResponsesStreamPayload
                && request != null
                && request.isStream()
                && request.getProtocol() == ProxyProtocol.RESPONSES;
    }

    private void appendStreamEvent(StringBuilder payloadBuilder, StreamEvent event) {
        if (payloadBuilder == null || event == null) {
            return;
        }
        List<String> comments = event.getComments();
        if (comments != null) {
            for (String comment : comments) {
                payloadBuilder.append(':');
                if (comment != null && !comment.isEmpty()) {
                    payloadBuilder.append(comment);
                }
                payloadBuilder.append('\n');
            }
        }
        if (event.getId() != null && !event.getId().isBlank()) {
            payloadBuilder.append("id: ").append(event.getId()).append('\n');
        }
        if (event.getRetry() != null) {
            payloadBuilder.append("retry: ").append(event.getRetry()).append('\n');
        }
        if (event.getEvent() != null && !event.getEvent().isBlank()) {
            payloadBuilder.append("event: ").append(event.getEvent()).append('\n');
        }
        List<String> dataLines = event.getDataLines();
        if (dataLines != null) {
            for (String dataLine : dataLines) {
                payloadBuilder.append("data: ");
                if (dataLine != null) {
                    payloadBuilder.append(dataLine);
                }
                payloadBuilder.append('\n');
            }
        }
        payloadBuilder.append('\n');
    }

    private void logUpstreamResponsesStreamPayload(ProviderInvokeContext context, StringBuilder payloadBuilder) {
        if (payloadBuilder == null || payloadBuilder.isEmpty()) {
            return;
        }
        String requestId = context == null ? null : context.getRequestId();
        String providerCode = context == null ? null : context.getProviderCode();
        String modelCode = context == null ? null : context.getModelCode();
        log.info("Upstream responses stream payload captured once. requestId={}, providerCode={}, modelCode={}\n{}",
                requestId,
                providerCode,
                modelCode,
                payloadBuilder);
    }
}


