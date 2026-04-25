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
import site.xlinks.ai.router.service.routing.ProxyErrors;
import site.xlinks.ai.router.service.routing.ProxyRoutingPipeline;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;

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
    private static final long STREAM_DISPATCH_JOIN_TIMEOUT_MS = 3_000L;

    private final CustomerTokenAuthService customerTokenAuthService;
    private final ProviderProtocolAdapterFactory adapterFactory;
    private final RouteCacheService routeCacheService;
    private final UsageRecordService usageRecordService;
    private final UsageExtractor usageExtractor;
    private final ProxyRoutingPipeline proxyRoutingPipeline;
    private final ProviderConcurrencyGuard providerConcurrencyGuard;
    @Qualifier("sseTaskExecutor")
    private final TaskExecutor sseTaskExecutor;

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

    public JsonNode forwardDirect(String token, ProxyRequest request) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        ScheduledFuture<?> renewFuture = null;
        Throwable unexpectedError = null;
        ProxyRequestTrace.begin(requestId, request, traceTimelineEnabled, traceStreamEventPreviewLimit);
        ProxyRequestTrace.addRouteEvent("收到代理请求，开始处理");
        try {
            context = buildContext(token, request, requestId);
            ProxyRequestTrace.addTimelineEvent("路由完成", "调用上下文已构建");
            renewFuture = providerConcurrencyGuard.scheduleAutoRenew(context);
            ProxyRequestTrace.addTimelineEvent("并发许可", "已启动自动续租");
            ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
            ProxyRequestTrace.addTimelineEvent("上游请求", buildUpstreamRequestDetail(context, request, false));
            JsonNode response;
            try {
                response = adapter.forwardDirect(request, context);
            } catch (UpstreamTimeoutException ex) {
                routeCacheService.recordProviderFailure(context.getProviderId());
                throw ex;
            } catch (Exception ex) {
                routeCacheService.recordProviderFailure(context.getProviderId());
                throw ex;
            }
            routeCacheService.clearProviderFailure(context.getProviderId());
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
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (UpstreamTimeoutException e) {
            throw handleTimeoutError(context, e, startAt);
        } catch (Exception e) {
            unexpectedError = e;
            throw handleUnexpectedError("Proxy request failed", context, e, startAt);
        } finally {
            providerConcurrencyGuard.cancelAutoRenew(renewFuture);
            providerConcurrencyGuard.releaseQuietly(context);
            emitTraceSummary(unexpectedError);
        }
    }

    public void forwardStream(String token,
                              ProxyRequest request,
                              Consumer<StreamEvent> onEvent) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        ScheduledFuture<?> renewFuture = null;
        boolean captureUpstreamStreamPayload = shouldCaptureUpstreamResponsesStreamPayload(request);
        StringBuilder upstreamStreamPayloadBuilder = captureUpstreamStreamPayload ? new StringBuilder() : null;
        Throwable unexpectedError = null;
        ProxyRequestTrace.begin(requestId, request, traceTimelineEnabled, traceStreamEventPreviewLimit);
        ProxyRequestTrace.addRouteEvent("收到代理请求，开始处理");
        try {
            context = buildContext(token, request, requestId);
            ProxyRequestTrace.addTimelineEvent("路由完成", "调用上下文已构建");
            renewFuture = providerConcurrencyGuard.scheduleAutoRenew(context);
            ProxyRequestTrace.addTimelineEvent("并发许可", "已启动自动续租");
            ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
            ProxyRequestTrace.addTimelineEvent("上游请求", buildUpstreamRequestDetail(context, request, true));
            AtomicReference<UsageMetrics> usageMetricsRef = new AtomicReference<>();
            AtomicReference<Integer> responseMsRef = new AtomicReference<>();
            int queueCapacity = streamDispatchQueueCapacity <= 0
                    ? DEFAULT_STREAM_DISPATCH_QUEUE_CAPACITY
                    : streamDispatchQueueCapacity;
            ArrayBlockingQueue<StreamDispatchItem> streamDispatchQueue = new ArrayBlockingQueue<>(queueCapacity);
            AtomicReference<RuntimeException> writerFailureRef = new AtomicReference<>();
            CountDownLatch writerDone = new CountDownLatch(1);
            String modelProvider = context.getModelProvider();
            sseTaskExecutor.execute(() -> runStreamWriter(streamDispatchQueue, onEvent, writerFailureRef, writerDone));
            try {
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
            } catch (ClientAbortException ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                throw ex;
            } catch (StreamFirstResponseTimeoutException | StreamIdleTimeoutException | UpstreamTimeoutException ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                routeCacheService.recordProviderFailure(context.getProviderId());
                throw ex;
            } catch (Exception ex) {
                enqueueStreamDispatchEnd(streamDispatchQueue, writerFailureRef);
                waitStreamWriterDone(writerDone, writerFailureRef);
                routeCacheService.recordProviderFailure(context.getProviderId());
                throw ex;
            }
            routeCacheService.clearProviderFailure(context.getProviderId());
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
            recordBusinessError(context, e, startAt);
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
            providerConcurrencyGuard.cancelAutoRenew(renewFuture);
            providerConcurrencyGuard.releaseQuietly(context);
            emitTraceSummary(unexpectedError);
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
        String finishReason = e.getCode() == ErrorCode.RATE_LIMITED.getCode() ? "rate_limited" : "business_error";
        recordError(context, 500, String.valueOf(e.getCode()), e.getMessage(), startAt, finishReason);
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
        if (context == null) {
            return;
        }
        usageRecordService.recordError(
                context,
                responseStatus,
                errorCode,
                errorMessage,
                System.currentTimeMillis() - startAt,
                finishReason
        );
        ProxyRequestTrace.markFailure(context, finishReason, errorCode, errorMessage);
        ProxyRequestTrace.addRouteEvent("请求结束，结果=" + finishReason + "，错误码=" + errorCode);
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
            if (!writerDone.await(STREAM_DISPATCH_JOIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                RuntimeException timeoutError = new RuntimeException("Timed out waiting for stream writer to finish");
                writerFailureRef.compareAndSet(null, timeoutError);
                log.warn("Timed out waiting for stream writer to finish");
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
