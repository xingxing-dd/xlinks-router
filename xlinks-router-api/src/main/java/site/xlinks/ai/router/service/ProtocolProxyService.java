package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
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
import site.xlinks.ai.router.service.RetryRouteExclusions;
import site.xlinks.ai.router.service.routing.ProxyErrors;
import site.xlinks.ai.router.service.routing.ProxyRoutingPipeline;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;
import site.xlinks.ai.router.service.routing.RoutingStepException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrates routing, provider invocation, permit lifecycle, and usage recording.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolProxyService {

    private static final int DEFAULT_STREAM_DISPATCH_QUEUE_CAPACITY = 128;
    private static final long STREAM_DISPATCH_OFFER_TIMEOUT_MS = 200L;
    private static final int MAX_UPSTREAM_ATTEMPTS = 3;
    private final CustomerTokenAuthService customerTokenAuthService;
    private final ProviderProtocolAdapterFactory adapterFactory;
    private final RouteCacheService routeCacheService;
    private final UsageRecordService usageRecordService;
    private final UsageExtractor usageExtractor;
    private final ProxyRoutingPipeline proxyRoutingPipeline;
    private final ProviderConcurrencyGuard providerConcurrencyGuard;
    @Qualifier("sseTaskExecutor")
    private final TaskExecutor sseTaskExecutor;
    @Qualifier("sseWriterTaskExecutor")
    private final TaskExecutor sseWriterTaskExecutor;

    @Value("${xlinks.router.debug.log-upstream-responses-stream-payload:false}")
    private boolean logUpstreamResponsesStreamPayload;
    @Value("${xlinks.router.debug.trace-timeline-enabled:false}")
    private boolean traceTimelineEnabled;
    @Value("${xlinks.router.debug.trace-stream-event-preview-limit:120}")
    private int traceStreamEventPreviewLimit;
    @Value("${xlinks.router.debug.trace-log-provider-token-plain:false}")
    private boolean traceLogProviderTokenPlain;
    @Value("${xlinks.router.stream.dispatch.queue-capacity:128}")
    private int streamDispatchQueueCapacity;
    @Value("${xlinks.router.stream.dispatch.join-timeout-ms:0}")
    private long streamDispatchJoinTimeoutMs;

    public JsonNode forwardDirect(String token, ProxyRequest request) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        Throwable unexpectedError = null;
        ProxyRequestTrace.begin(requestId, request, traceTimelineEnabled, traceStreamEventPreviewLimit);
        ProxyRequestTrace.addRouteEvent("收到代理请求，开始处理");
        try {
            DirectInvokeResult invokeResult = forwardDirectWithRetry(token, request, requestId);
            context = invokeResult.context();
            JsonNode response = invokeResult.response();
            ProxyRequestTrace.addTimelineEvent("上游响应", "直连响应已返回");
            ProxyRequestTrace.markFirstResponse();
            UsageMetrics usageMetrics = usageExtractor.extract(response, context.getModelProvider());
            usageRecordService.recordAsync(
                    context,
                    usageMetrics,
                    System.currentTimeMillis() - startAt,
                    null,
                    null,
                    "success"
            );
            ProxyRequestTrace.markSuccess(context, usageMetrics);
            return response;
        } catch (BusinessException e) {
            recordBusinessError(context, extractRoutingContext(e), e, startAt);
            throw e;
        } catch (UpstreamTimeoutException e) {
            throw handleTimeoutError(context, e, startAt);
        } catch (Exception e) {
            unexpectedError = e;
            throw handleUnexpectedError("Proxy request failed", context, e, startAt);
        } finally {
            emitTraceSummary(unexpectedError);
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
        Throwable unexpectedError = null;
        ProxyRequestTrace.begin(requestId, request, traceTimelineEnabled, traceStreamEventPreviewLimit);
        ProxyRequestTrace.addRouteEvent("收到代理请求，开始处理");
        try {
            StreamInvokeResult streamInvokeResult = forwardStreamWithRetry(token, request, requestId, onEvent, upstreamStreamPayloadBuilder, startAt);
            context = streamInvokeResult.context();
            AtomicReference<UsageMetrics> usageMetricsRef = streamInvokeResult.usageMetricsRef();
            AtomicReference<Integer> responseMsRef = streamInvokeResult.responseMsRef();
            usageRecordService.recordAsync(
                    context,
                    usageMetricsRef.get(),
                    System.currentTimeMillis() - startAt,
                    responseMsRef.get(),
                    null,
                    null,
                    "success"
            );
            ProxyRequestTrace.markSuccess(context, usageMetricsRef.get());
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
        } catch (ClientAbortException e) {
            ProxyRequestTrace.addRouteEvent("客户端主动断开连接");
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            recordError(context, 500, "CLIENT_ABORT", e.getMessage(), startAt, "client_abort");
        } catch (BusinessException e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            recordBusinessError(context, extractRoutingContext(e), e, startAt);
            throw e;
        } catch (StreamFirstResponseTimeoutException e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            throw handleStreamTimeoutError(context, e, startAt, "stream_first_event_timeout");
        } catch (StreamIdleTimeoutException e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            throw handleStreamTimeoutError(context, e, startAt, "stream_idle_timeout");
        } catch (UpstreamTimeoutException e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            throw handleTimeoutError(context, e, startAt);
        } catch (Exception e) {
            logUpstreamResponsesStreamPayload(context, upstreamStreamPayloadBuilder);
            unexpectedError = e;
            throw handleUnexpectedError("Proxy stream failed", context, e, startAt);
        } finally {
            emitTraceSummary(unexpectedError);
        }
    }

    public Object listModels(String token) {
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);
        List<Model> models = routeCacheService.listModels();
        List<Object> modelList = models.stream()
                .filter(model -> model.getModelCode() != null && !model.getModelCode().isBlank())
                .filter(model -> customerTokenAuthService.hasPermissionForModel(customerToken, model.getModelCode()))
                .map(model -> Map.of(
                        "id", model.getModelCode(),
                        "object", "model",
                        "created", 0,
                        "owned_by", "openai"
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
        return buildContext(token, request, requestId, new RetryRouteExclusions());
    }

    private ProviderInvokeContext buildContext(String token,
                                               ProxyRequest request,
                                               String requestId,
                                               RetryRouteExclusions exclusions) {
        RoutingBuildContext routingContext = proxyRoutingPipeline.resolve(
                token,
                request,
                requestId,
                exclusions.getProviderIds(),
                exclusions.getProviderTokenIds()
        );
        CustomerToken customerToken = routingContext.getCustomerToken();
        UsageDecision usageDecision = routingContext.getUsageDecision();
        Model model = routingContext.getModel();
        Provider provider = routingContext.getProvider();
        ProviderModel providerModel = routingContext.getProviderModel();
        ProviderToken providerToken = routingContext.getProviderToken();
        ProviderPermitLease permitLease = routingContext.getProviderPermitLease();
        String endpointCode = request.getProtocol().getCode();

        String upstreamModelCode = providerModel.getProviderModelCode();
        if (upstreamModelCode == null || upstreamModelCode.isBlank()) {
            upstreamModelCode = model.getModelCode();
        }
        ProxyRuntimePolicy runtimePolicy = permitLease == null
                ? new ProxyRuntimePolicy(false, 0, 0, 20000, 20000, 20000, 30000, 10000)
                : permitLease.runtimePolicy();
        ProviderInvokeContext invokeContext = ProviderInvokeContext.builder()
                .requestId(requestId)
                .customerName(usageDecision.getCustomerName())
                .customerTokenName(customerToken.getTokenName())
                .planName(usageDecision.getPlanName())
                .providerId(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .baseUrl(provider.getBaseUrl())
                .providerToken(providerToken.getTokenValue())
                .providerTokenId(providerToken.getId())
                .providerTokenName(providerToken.getTokenName())
                .providerPermitId(permitLease == null ? null : permitLease.permitId())
                .customerToken(token)
                .endpointCode(endpointCode)
                .modelId(model.getId())
                .modelCode(model.getModelCode())
                .modelName(model.getModelName())
                .inputPrice(model.getInputPrice())
                .cacheHitPrice(model.getCacheHitPrice())
                .outputPrice(model.getOutputPrice())
                .multiplier(usageDecision.getMultiplier())
                .modelProvider(model.getModelProvider())
                .providerModel(upstreamModelCode)
                .providerModelName(providerModel.getProviderModelName())
                .planId(usageDecision.getPlanId())
                .customerTokenId(customerToken.getId())
                .accountId(customerToken.getAccountId())
                .customerModel(request.getModel())
                .requestTimeoutMs(runtimePolicy.requestTimeoutMs())
                .streamFirstResponseTimeoutMs(runtimePolicy.streamFirstResponseTimeoutMs())
                .streamIdleTimeoutMs(runtimePolicy.streamIdleTimeoutMs())
                .sessionLeaseMs(runtimePolicy.sessionLeaseMs())
                .sessionRenewIntervalMs(runtimePolicy.sessionRenewIntervalMs())
                .build();
        ProxyRequestTrace.markInvokeContext(invokeContext);
        ProxyRequestTrace.addRouteEvent("路由解析完成，已确定上游 provider 和 token");
        return invokeContext;
    }

    private DirectInvokeResult forwardDirectWithRetry(String token,
                                                      ProxyRequest request,
                                                      String requestId) {
        RetryRouteExclusions exclusions = new RetryRouteExclusions();
        ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
        UpstreamProviderException lastRetryableFailure = null;
        for (int attempt = 1; attempt <= MAX_UPSTREAM_ATTEMPTS; attempt++) {
            ProviderInvokeContext context = buildContext(token, request, requestId, exclusions);
            ScheduledFuture<?> renewFuture = null;
            try {
                logRetryAttempt("直连", attempt, context, exclusions);
                ProxyRequestTrace.addTimelineEvent("路由完成", "调用上下文已构建");
                renewFuture = providerConcurrencyGuard.scheduleAutoRenew(context);
                ProxyRequestTrace.addTimelineEvent("并发许可", "已启动自动续租");
                ProxyRequestTrace.addTimelineEvent("上游请求", buildUpstreamRequestDetail(context, request, false));
                JsonNode response = adapter.forwardDirect(request, context);
                clearSelectedRouteFailure(context);
                if (attempt > 1) {
                    ProxyRequestTrace.addRouteEvent("重试成功，结束重试链路("
                            + buildRetryRouteDetail(attempt, context) + ")");
                }
                return new DirectInvokeResult(context, response);
            } catch (UpstreamTimeoutException ex) {
                recordSelectedRouteFailure(context);
                throw ex;
            } catch (UpstreamProviderException ex) {
                recordSelectedRouteFailure(context);
                ProxyRequestTrace.addRouteEvent("上游失败("
                        + buildRetryFailureDetail(attempt, context, ex) + ")");
                if (!shouldRetryProviderFailure(ex, attempt)) {
                    ProxyRequestTrace.addRouteEvent("不再重试("
                            + buildRetryStopDetail(attempt, context, ex) + ")");
                    throw ex;
                }
                lastRetryableFailure = ex;
                exclusions.exclude(context);
                ProxyRequestTrace.addRouteEvent("准备重试下一个 provider/token("
                        + buildRetryPlanDetail(attempt, context, ex, exclusions) + ")");
            } catch (Exception ex) {
                recordSelectedRouteFailure(context);
                throw ex;
            } finally {
                providerConcurrencyGuard.cancelAutoRenew(renewFuture);
                providerConcurrencyGuard.releaseQuietly(context);
            }
        }
        throw lastRetryableFailure == null
                ? new RuntimeException("Upstream retry exhausted without a captured provider failure")
                : lastRetryableFailure;
    }

    private StreamInvokeResult forwardStreamWithRetry(String token,
                                                      ProxyRequest request,
                                                      String requestId,
                                                      Consumer<StreamEvent> onEvent,
                                                      StringBuilder upstreamStreamPayloadBuilder,
                                                      long startAt) {
        RetryRouteExclusions exclusions = new RetryRouteExclusions();
        ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
        UpstreamProviderException lastRetryableFailure = null;
        for (int attempt = 1; attempt <= MAX_UPSTREAM_ATTEMPTS; attempt++) {
            ProviderInvokeContext context = buildContext(token, request, requestId, exclusions);
            ScheduledFuture<?> renewFuture = null;
            AtomicReference<UsageMetrics> usageMetricsRef = new AtomicReference<>();
            AtomicReference<Integer> responseMsRef = new AtomicReference<>();
            int queueCapacity = streamDispatchQueueCapacity <= 0
                    ? DEFAULT_STREAM_DISPATCH_QUEUE_CAPACITY
                    : streamDispatchQueueCapacity;
            ArrayBlockingQueue<StreamDispatchItem> streamDispatchQueue = new ArrayBlockingQueue<>(queueCapacity);
            AtomicReference<RuntimeException> writerFailureRef = new AtomicReference<>();
            CountDownLatch writerDone = new CountDownLatch(1);
            try {
                logRetryAttempt("流式", attempt, context, exclusions);
                ProxyRequestTrace.addTimelineEvent("路由完成", "调用上下文已构建");
                renewFuture = providerConcurrencyGuard.scheduleAutoRenew(context);
                ProxyRequestTrace.addTimelineEvent("并发许可", "已启动自动续租");
                ProxyRequestTrace.addTimelineEvent("上游请求", buildUpstreamRequestDetail(context, request, true));
                String modelProvider = context.getModelProvider();
                sseWriterTaskExecutor.execute(() -> runStreamWriter(streamDispatchQueue, onEvent, writerFailureRef, writerDone));
                adapter.forwardStream(request, context, event -> {
                    appendStreamEvent(upstreamStreamPayloadBuilder, event);
                    ProxyRequestTrace.recordStreamEvent(event);
                    if (isFirstResponseDataEvent(event)) {
                        long firstResponseMs = System.currentTimeMillis() - startAt;
                        int normalized = firstResponseMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(firstResponseMs, 0L);
                        responseMsRef.compareAndSet(null, normalized);
                        ProxyRequestTrace.markFirstResponse();
                    }
                    UsageMetrics usageMetrics = usageExtractor.extract(event, modelProvider);
                    if (usageMetrics != null) {
                        usageMetricsRef.set(usageMetrics);
                    }
                    enqueueStreamEvent(streamDispatchQueue, event, writerFailureRef);
                });
                ProxyRequestTrace.addTimelineEvent("流式读取", "上游流读取完成");
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                RuntimeException writerFailure = writerFailureRef.get();
                if (writerFailure != null) {
                    throw writerFailure;
                }
                clearSelectedRouteFailure(context);
                if (attempt > 1) {
                    ProxyRequestTrace.addRouteEvent("重试成功，结束重试链路("
                            + buildRetryRouteDetail(attempt, context) + ")");
                }
                return new StreamInvokeResult(context, usageMetricsRef, responseMsRef);
            } catch (ClientAbortException ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                throw ex;
            } catch (StreamFirstResponseTimeoutException | StreamIdleTimeoutException | UpstreamTimeoutException ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                recordSelectedRouteFailure(context);
                throw ex;
            } catch (UpstreamProviderException ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                recordSelectedRouteFailure(context);
                ProxyRequestTrace.addRouteEvent("流式上游失败("
                        + buildRetryFailureDetail(attempt, context, ex) + ")");
                if (!shouldRetryProviderFailure(ex, attempt) || responseMsRef.get() != null) {
                    ProxyRequestTrace.addRouteEvent("流式不再重试("
                            + buildRetryStopDetail(attempt, context, ex)
                            + ", firstResponseReceived=" + (responseMsRef.get() != null) + ")");
                    throw ex;
                }
                lastRetryableFailure = ex;
                exclusions.exclude(context);
                ProxyRequestTrace.addRouteEvent("流式首包前准备重试下一个 provider/token("
                        + buildRetryPlanDetail(attempt, context, ex, exclusions) + ")");
            } catch (Exception ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                recordSelectedRouteFailure(context);
                throw ex;
            } finally {
                providerConcurrencyGuard.cancelAutoRenew(renewFuture);
                providerConcurrencyGuard.releaseQuietly(context);
            }
        }
        throw lastRetryableFailure == null
                ? new RuntimeException("Upstream stream retry exhausted without a captured provider failure")
                : lastRetryableFailure;
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

    private void recordBusinessError(ProviderInvokeContext context,
                                     RoutingBuildContext routingContext,
                                     BusinessException e,
                                     long startAt) {
        String finishReason = classifyBusinessFinishReason(e);
        recordError(context, routingContext, 500, String.valueOf(e.getCode()), e.getMessage(), startAt, finishReason);
    }

    private BusinessException handleTimeoutError(ProviderInvokeContext context,
                                                 UpstreamTimeoutException e,
                                                 long startAt) {
        recordError(context, 500, ErrorCode.UPSTREAM_TIMEOUT.name(), e.getMessage(), startAt, "upstream_timeout");
        return ProxyErrors.upstreamTimeout();
    }

    private BusinessException handleStreamTimeoutError(ProviderInvokeContext context,
                                                       RuntimeException e,
                                                       long startAt,
                                                       String finishReason) {
        recordError(context, 500, ErrorCode.UPSTREAM_TIMEOUT.name(), e.getMessage(), startAt, finishReason);
        return ProxyErrors.upstreamTimeout();
    }

    private BusinessException handleUnexpectedError(String logMessage,
                                                    ProviderInvokeContext context,
                                                    Exception e,
                                                    long startAt) {
        recordError(context, 500, ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), startAt, "internal_error");
        return ProxyErrors.requestProcessingFailed(e.getMessage());
    }

    private void recordError(ProviderInvokeContext context,
                             int responseStatus,
                             String errorCode,
                             String errorMessage,
                             long startAt,
                             String finishReason) {
        recordError(context, null, responseStatus, errorCode, errorMessage, startAt, finishReason);
    }

    private void recordError(ProviderInvokeContext context,
                             RoutingBuildContext routingContext,
                             int responseStatus,
                             String errorCode,
                             String errorMessage,
                             long startAt,
                             String finishReason) {
        if (context != null) {
            usageRecordService.recordError(
                    context,
                    responseStatus,
                    errorCode,
                    errorMessage,
                    System.currentTimeMillis() - startAt,
                    finishReason
            );
        } else if (routingContext != null && routingContext.getCustomerToken() != null) {
            usageRecordService.recordRoutingError(
                    routingContext,
                    responseStatus,
                    errorCode,
                    errorMessage,
                    System.currentTimeMillis() - startAt,
                    finishReason
            );
        }
        ProxyRequestTrace.markFailure(context, finishReason, errorCode, errorMessage);
        ProxyRequestTrace.addRouteEvent("请求结束，结果=" + finishReason + "，错误码=" + errorCode);
    }

    private RoutingBuildContext extractRoutingContext(BusinessException e) {
        if (e instanceof RoutingStepException routingStepException) {
            return routingStepException.getRoutingContext();
        }
        return null;
    }

    private String classifyBusinessFinishReason(BusinessException e) {
        if (e == null) {
            return "business_error";
        }
        String message = e.getMessage();
        if (message == null) {
            return e.getCode() == ErrorCode.RATE_LIMITED.getCode() ? "rate_limited" : "business_error";
        }
        if ("No available plan or wallet balance".equals(message)) {
            return "entitlement_unavailable";
        }
        if ("Customer token total quota reached".equals(message)) {
            return "token_total_quota_reached";
        }
        if ("Customer token daily quota reached".equals(message)) {
            return "token_daily_quota_reached";
        }
        if ("Customer token model is not allowed".equals(message)) {
            return "token_model_not_allowed";
        }
        if (message.startsWith("Model does not exist or is unavailable: ")) {
            return "model_unavailable";
        }
        if (message.startsWith("No available provider mapping for model and protocol: ")) {
            return "provider_mapping_unavailable";
        }
        if (message.startsWith("No available provider token for model and protocol: ")) {
            return "provider_token_unavailable";
        }
        if ("Provider token concurrency limit reached".equals(message)) {
            return "provider_rate_limited";
        }
        return e.getCode() == ErrorCode.RATE_LIMITED.getCode() ? "rate_limited" : "business_error";
    }

    private void recordSelectedRouteFailure(ProviderInvokeContext context) {
        if (context == null) {
            return;
        }
        if (context.getProviderTokenId() != null) {
            routeCacheService.recordProviderTokenFailure(context.getProviderTokenId());
            return;
        }
        routeCacheService.recordProviderFailure(context.getProviderId());
    }

    private void clearSelectedRouteFailure(ProviderInvokeContext context) {
        if (context == null) {
            return;
        }
        if (context.getProviderTokenId() != null) {
            routeCacheService.clearProviderTokenFailure(context.getProviderTokenId());
        }
        routeCacheService.clearProviderFailure(context.getProviderId());
    }

    private boolean shouldRetryProviderFailure(UpstreamProviderException exception, int attempt) {
        return exception != null
                && exception.isRetryable()
                && attempt < MAX_UPSTREAM_ATTEMPTS;
    }

    private void logRetryAttempt(String mode,
                                 int attempt,
                                 ProviderInvokeContext context,
                                 RetryRouteExclusions exclusions) {
        if (attempt <= 1) {
            return;
        }
        ProxyRequestTrace.addRouteEvent(mode + "重试开始("
                + buildRetryRouteDetail(attempt, context)
                + ", excludedProviderIds=" + exclusions.getProviderIds()
                + ", excludedProviderTokenIds=" + exclusions.getProviderTokenIds()
                + ")");
    }

    private String buildRetryRouteDetail(int attempt, ProviderInvokeContext context) {
        return "attempt=" + attempt + "/" + MAX_UPSTREAM_ATTEMPTS
                + ", providerId=" + nullSafe(context == null ? null : context.getProviderId())
                + ", providerCode=" + nullSafe(context == null ? null : context.getProviderCode())
                + ", providerTokenId=" + nullSafe(context == null ? null : context.getProviderTokenId())
                + ", providerModel=" + nullSafe(context == null ? null : context.getProviderModel())
                + ", modelCode=" + nullSafe(context == null ? null : context.getModelCode())
                + ", baseUrl=" + nullSafe(context == null ? null : context.getBaseUrl());
    }

    private String buildRetryFailureDetail(int attempt,
                                           ProviderInvokeContext context,
                                           UpstreamProviderException exception) {
        return buildRetryRouteDetail(attempt, context)
                + ", statusCode=" + nullSafe(exception == null ? null : exception.getStatusCode())
                + ", retryable=" + (exception != null && exception.isRetryable())
                + ", bodyPreview=" + abbreviateForLog(exception == null ? null : exception.getResponseBody(), 240);
    }

    private String buildRetryPlanDetail(int attempt,
                                        ProviderInvokeContext context,
                                        UpstreamProviderException exception,
                                        RetryRouteExclusions exclusions) {
        return buildRetryFailureDetail(attempt, context, exception)
                + ", nextAttempt=" + (attempt + 1) + "/" + MAX_UPSTREAM_ATTEMPTS
                + ", excludedProviderIds=" + exclusions.getProviderIds()
                + ", excludedProviderTokenIds=" + exclusions.getProviderTokenIds();
    }

    private String buildRetryStopDetail(int attempt,
                                        ProviderInvokeContext context,
                                        UpstreamProviderException exception) {
        return buildRetryFailureDetail(attempt, context, exception)
                + ", maxAttemptsReached=" + (attempt >= MAX_UPSTREAM_ATTEMPTS);
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

    private String buildUpstreamRequestDetail(ProviderInvokeContext context, ProxyRequest request, boolean stream) {
        String url = buildUpstreamUrl(context, request);
        String token = context == null ? null : context.getProviderToken();
        String tokenForLog = formatTokenForLog(token);
        Integer requestTimeoutMs = context == null ? null : context.getRequestTimeoutMs();
        Integer firstTimeoutMs = context == null ? null : context.getStreamFirstResponseTimeoutMs();
        Integer idleTimeoutMs = context == null ? null : context.getStreamIdleTimeoutMs();
        return "url=" + nullSafe(url)
                + ", stream=" + stream
                + ", providerToken=" + tokenForLog
                + ", requestTimeoutMs=" + nullSafe(requestTimeoutMs)
                + ", streamFirstResponseTimeoutMs=" + nullSafe(firstTimeoutMs)
                + ", streamIdleTimeoutMs=" + nullSafe(idleTimeoutMs);
    }

    private String buildUpstreamUrl(ProviderInvokeContext context, ProxyRequest request) {
        if (context == null || request == null || request.getProtocol() == null) {
            return null;
        }
        String baseUrl = context.getBaseUrl();
        String path = request.getProtocol().getProviderPath();
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private String abbreviateForLog(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        if (maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private record DirectInvokeResult(ProviderInvokeContext context, JsonNode response) {
    }

    private record StreamInvokeResult(ProviderInvokeContext context,
                                      AtomicReference<UsageMetrics> usageMetricsRef,
                                      AtomicReference<Integer> responseMsRef) {
    }

    private String formatTokenForLog(String token) {
        if (token == null || token.isBlank()) {
            return "-";
        }
        if (traceLogProviderTokenPlain) {
            return token;
        }
        if (token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
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

    private void runStreamWriter(ArrayBlockingQueue<StreamDispatchItem> streamDispatchQueue,
                                 Consumer<StreamEvent> onEvent,
                                 AtomicReference<RuntimeException> writerFailureRef,
                                 CountDownLatch writerDone) {
        try {
            while (true) {
                StreamDispatchItem item = streamDispatchQueue.take();
                if (item.endSignal()) {
                    return;
                }
                onEvent.accept(item.event());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writerFailureRef.compareAndSet(null, new RuntimeException("Stream writer interrupted", e));
        } catch (RuntimeException e) {
            writerFailureRef.compareAndSet(null, e);
        } finally {
            writerDone.countDown();
        }
    }

    private void enqueueStreamEvent(ArrayBlockingQueue<StreamDispatchItem> streamDispatchQueue,
                                    StreamEvent event,
                                    AtomicReference<RuntimeException> writerFailureRef) {
        if (event == null) {
            return;
        }
        offerStreamDispatchItem(streamDispatchQueue, StreamDispatchItem.event(event), writerFailureRef);
    }

    private void enqueueStreamDispatchEnd(ArrayBlockingQueue<StreamDispatchItem> streamDispatchQueue,
                                          AtomicReference<RuntimeException> writerFailureRef) {
        offerStreamDispatchItem(streamDispatchQueue, StreamDispatchItem.end(), writerFailureRef);
    }

    private void offerStreamDispatchItem(ArrayBlockingQueue<StreamDispatchItem> streamDispatchQueue,
                                         StreamDispatchItem item,
                                         AtomicReference<RuntimeException> writerFailureRef) {
        int offerRetry = 0;
        while (true) {
            RuntimeException writerFailure = writerFailureRef.get();
            if (writerFailure != null) {
                log.warn("Stream writer failed before enqueue, itemType={}, error={}",
                        item.endSignal() ? "end" : "event",
                        writerFailure.getMessage());
                throw writerFailure;
            }
            try {
                if (streamDispatchQueue.offer(item, STREAM_DISPATCH_OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    return;
                }
                offerRetry++;
                if (offerRetry == 1 || offerRetry % 10 == 0) {
                    log.warn("Stream dispatch queue is full, retrying enqueue. itemType={}, queueSize={}, capacity={}, retries={}",
                            item.endSignal() ? "end" : "event",
                            streamDispatchQueue.size(),
                            streamDispatchQueue.remainingCapacity() + streamDispatchQueue.size(),
                            offerRetry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while dispatching stream event", e);
            }
        }
    }

    private void waitStreamWriterDone(CountDownLatch writerDone,
                                      AtomicReference<RuntimeException> writerFailureRef) {
        try {
            if (streamDispatchJoinTimeoutMs <= 0) {
                writerDone.await();
                return;
            }
            if (!writerDone.await(streamDispatchJoinTimeoutMs, TimeUnit.MILLISECONDS)) {
                RuntimeException timeoutError = new RuntimeException("Timed out waiting for stream writer to finish");
                writerFailureRef.compareAndSet(null, timeoutError);
                log.warn("Timed out waiting for stream writer to finish, timeoutMs={}", streamDispatchJoinTimeoutMs);
                throw timeoutError;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            RuntimeException interruptedError = new RuntimeException("Interrupted while waiting stream writer", e);
            writerFailureRef.compareAndSet(null, interruptedError);
            throw interruptedError;
        }
    }

    private void emitTraceSummary(Throwable unexpectedError) {
        String summary = ProxyRequestTrace.buildSummary();
        if (summary == null) {
            return;
        }
        try {
            if (unexpectedError != null) {
                log.error(summary, unexpectedError);
                return;
            }
            if (summary.contains("success=true")) {
                log.info(summary);
                return;
            }
            log.warn(summary);
        } finally {
            ProxyRequestTrace.clear();
        }
    }

    private record StreamDispatchItem(StreamEvent event, boolean endSignal) {

        private static StreamDispatchItem event(StreamEvent event) {
            return new StreamDispatchItem(event, false);
        }

        private static StreamDispatchItem end() {
            return new StreamDispatchItem(null, true);
        }
    }
}
