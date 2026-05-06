package site.xlinks.ai.router.app.forwarding;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.model.ForwardingAsyncStreamBody;
import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.app.forwarding.model.ForwardingNextAction;
import site.xlinks.ai.router.app.forwarding.model.ForwardingStage;
import site.xlinks.ai.router.app.forwarding.model.ForwardingUsageContext;
import site.xlinks.ai.router.app.forwarding.model.ProviderPermitLease;
import site.xlinks.ai.router.app.forwarding.model.ProviderRuntimePolicy;
import site.xlinks.ai.router.app.forwarding.model.UsageMetrics;
import site.xlinks.ai.router.app.forwarding.retry.ForwardFailureAction;
import site.xlinks.ai.router.app.forwarding.retry.ForwardFailureRoutingStrategy;
import site.xlinks.ai.router.domain.provider.ProviderTokenSelectionService;
import site.xlinks.ai.router.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.domain.routing.model.RoutingDecisionStage;
import site.xlinks.ai.router.infrastructure.http.ProviderHttpForwardingExecutor;
import site.xlinks.ai.router.infrastructure.http.exception.UpstreamRetryableException;
import site.xlinks.ai.router.infrastructure.http.exception.UpstreamTimeoutException;
import site.xlinks.ai.router.infrastructure.http.model.ProviderStreamHandle;
import site.xlinks.ai.router.infrastructure.runtime.ProviderConcurrencyGuard;
import site.xlinks.ai.router.infrastructure.runtime.ProviderRuntimeStateService;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.io.InterruptedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class ForwardingExecutionService {

    /**
     * 流式转发只保留尾部窗口用于提取 usage，避免缓存整条 SSE 数据。
     */
    private static final int STREAM_TAIL_CAPTURE_BYTES = 256 * 1024;
    private static final int STREAM_PREFETCH_CHUNK_BYTES = 32 * 1024;

    private final ForwardingReadModelLoader forwardingReadModelLoader;
    private final ProviderHttpForwardingExecutor providerHttpForwardingExecutor;
    private final ProviderConcurrencyGuard providerConcurrencyGuard;
    private final ForwardFailureRoutingStrategy forwardFailureRoutingStrategy;
    private final ProviderRuntimeStateService providerRuntimeStateService;
    private final ForwardingUsageRecordService forwardingUsageRecordService;
    private final UsageExtractor usageExtractor;
    private final ProviderTokenSelectionService providerTokenSelectionService;

    @Value("${xlinks.router.forward.max-retry-attempts:3}")
    private int maxRetryAttempts;

    public Object execute(ForwardingDecision decision) {
        ensureUsageContext(decision);
        int normalizedMaxAttempts = Math.max(maxRetryAttempts, 1);
        Set<Long> excludedProviderIds = new HashSet<>();
        UpstreamRetryableException lastRetryableException = null;

        try {
            for (int attempt = 1; attempt <= normalizedMaxAttempts; attempt++) {
                ForwardAttemptContext attemptContext = selectAttemptTarget(decision, excludedProviderIds, attempt);
                if (attemptContext == null) {
                    break;
                }
                ForwardAttemptResult result = executeAttempt(decision, attemptContext, attempt, normalizedMaxAttempts);
                if (result.retryableFailure() != null) {
                    lastRetryableException = result.retryableFailure();
                    if (result.switchProvider()) {
                        excludedProviderIds.add(result.providerId());
                        continue;
                    }
                    return translateFinalRetryableFailure(decision, result.retryableFailure());
                }
                if (result.response() != null) {
                    return result.response();
                }
            }

            if (lastRetryableException != null) {
                return translateFinalRetryableFailure(decision, lastRetryableException);
            }
            throw recordAndReturn(
                    decision,
                    new BusinessException(ErrorCode.PROVIDER_TOKEN_UNAVAILABLE, "没有可用的服务商令牌")
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            recordFinalFailure(
                    decision,
                    String.valueOf(ErrorCode.SYSTEM_ERROR.getCode()),
                    valueOrDash(ex.getMessage())
            );
            throw ex;
        }
    }

    private ForwardAttemptContext selectAttemptTarget(ForwardingDecision decision,
                                                      Set<Long> excludedProviderIds,
                                                      int attempt) {
        List<ProviderModel> orderedProviderModels = decision.getRoutingPlan().getOrderedProviderModels();
        if (orderedProviderModels == null || orderedProviderModels.isEmpty()) {
            return null;
        }

        for (ProviderModel providerModel : orderedProviderModels) {
            if (providerModel == null || providerModel.getProviderId() == null) {
                continue;
            }
            Long providerId = providerModel.getProviderId();
            if (excludedProviderIds.contains(providerId)) {
                continue;
            }

            Provider provider = forwardingReadModelLoader.loadProvider(providerId);
            if (provider == null || provider.getStatus() == null || provider.getStatus() != 1) {
                continue;
            }

            ProviderTokenSelectionResult tokenSelectionResult = selectProviderToken(decision, provider, attempt);
            if (tokenSelectionResult.providerToken() == null) {
                continue;
            }

            RoutingDecision selectedRoute = RoutingDecision.builder()
                    .accountId(decision.getRoutingPlan().getAccountId())
                    .modelId(decision.getRoutingPlan().getModelId())
                    .preferredProviderId(decision.getRoutingPlan().getPreferredProviderId())
                    .provider(provider)
                    .providerModel(providerModel)
                    .providerToken(tokenSelectionResult.providerToken())
                    .decisionStage(RoutingDecisionStage.ROUTED)
                    .build();
            decision.setSelectedRoute(selectedRoute);
            decision.setUsageContext(buildUsageContext(decision, selectedRoute));
            decision.setStage(ForwardingStage.TARGET_SELECTED);
            decision.setNextAction(ForwardingNextAction.PERMIT_ACQUIRE);
            RequestChainLogCollector.bindRoutingDecision(selectedRoute);
            recordAttemptStarted(attempt, provider, tokenSelectionResult.providerToken(), tokenSelectionResult.providerPermitLease());
            return new ForwardAttemptContext(selectedRoute, tokenSelectionResult.providerPermitLease(), null);
        }
        return null;
    }

    private ProviderTokenSelectionResult selectProviderToken(ForwardingDecision decision,
                                                             Provider provider,
                                                             int attempt) {
        List<ProviderToken> providerTokens = forwardingReadModelLoader.loadProviderTokens(provider.getId());
        Set<Long> excludedTokenIds = new HashSet<>();
        while (true) {
            ProviderToken providerToken = providerTokenSelectionService.select(
                    provider,
                    filterExcludedProviderTokens(providerTokens, excludedTokenIds)
            );
            if (providerToken == null) {
                return new ProviderTokenSelectionResult(null);
            }
            ProviderPermitLease providerPermitLease = providerConcurrencyGuard.tryAcquire(
                    provider,
                    providerToken,
                    decision.getRequestId()
            );
            if (providerPermitLease != null) {
                RequestChainLogCollector.record(
                        RequestChainLogType.PERMIT_ATTEMPT_ACQUIRED,
                        attempt,
                        describeProvider(provider),
                        describeProviderToken(providerToken),
                        providerPermitLease.permitId()
                );
                return new ProviderTokenSelectionResult(providerToken, providerPermitLease);
            }
            RequestChainLogCollector.record(
                RequestChainLogType.PERMIT_ATTEMPT_FAILED,
                attempt,
                describeProvider(provider),
                describeProviderToken(providerToken),
                valueOrDash(provider.getAcquireTimeoutMs())
            );
            if (providerToken.getId() != null) {
                excludedTokenIds.add(providerToken.getId());
            }
        }
    }

    private List<ProviderToken> filterExcludedProviderTokens(List<ProviderToken> providerTokens,
                                                             Set<Long> excludedTokenIds) {
        if (providerTokens == null || providerTokens.isEmpty()) {
            return List.of();
        }
        if (excludedTokenIds == null || excludedTokenIds.isEmpty()) {
            return providerTokens;
        }
        return providerTokens.stream()
                .filter(token -> token == null
                        || token.getId() == null
                        || !excludedTokenIds.contains(token.getId()))
                .toList();
    }

    private ForwardAttemptResult executeAttempt(ForwardingDecision decision,
                                                ForwardAttemptContext attemptContext,
                                                int attempt,
                                                int maxAttempts) {
        boolean releaseInFinally = true;
        try {
            startForwardingSession(decision, attemptContext);
            Object response = providerHttpForwardingExecutor.forward(decision);
            if (isStreamingResponse(response)) {
                Object streamingResponse = wrapStreamingResponse(decision, response, attemptContext);
                providerRuntimeStateService.clearFailure(attemptContext.routingDecision());
                releaseInFinally = false;
                RequestChainLogCollector.record(RequestChainLogType.STREAMING_STARTED);
                return ForwardAttemptResult.success(streamingResponse);
            }
            providerRuntimeStateService.clearFailure(attemptContext.routingDecision());
            recordDirectResponse(decision, response);
            return ForwardAttemptResult.success(response);
        } catch (UpstreamRetryableException ex) {
            return handleRetryableFailure(decision, attemptContext, ex, attempt, maxAttempts);
        } finally {
            if (releaseInFinally) {
                releaseAttemptResources(decision, attemptContext);
            }
        }
    }

    private void startForwardingSession(ForwardingDecision decision, ForwardAttemptContext attemptContext) {
        decision.setProviderPermitLease(attemptContext.providerPermitLease());
        decision.setSessionStartedAtMs(System.currentTimeMillis());
        decision.setStage(ForwardingStage.PERMIT_ACQUIRED);
        decision.setNextAction(ForwardingNextAction.HTTP_FORWARDING);
        attemptContext.renewFuture = providerConcurrencyGuard.scheduleAutoRenew(
                decision.getRequestId(),
                attemptContext.routingDecision().getProvider(),
                attemptContext.routingDecision().getProviderToken(),
                attemptContext.providerPermitLease()
        );
        RequestChainLogCollector.record(
                RequestChainLogType.SESSION_STARTED,
                describeProvider(attemptContext.routingDecision().getProvider()),
                describeProviderToken(attemptContext.routingDecision().getProviderToken())
        );
    }

    private ForwardAttemptResult handleRetryableFailure(ForwardingDecision decision,
                                                        ForwardAttemptContext attemptContext,
                                                        UpstreamRetryableException failure,
                                                        int attempt,
                                                        int maxAttempts) {
        providerRuntimeStateService.recordFailure(attemptContext.routingDecision());
        boolean switchProvider = forwardFailureRoutingStrategy.decide(failure, attempt, maxAttempts)
                == ForwardFailureAction.SWITCH_PROVIDER;
        RequestChainLogCollector.record(
                RequestChainLogType.UPSTREAM_RETRYABLE_FAILURE,
                attempt,
                describeProvider(attemptContext.routingDecision().getProvider()),
                describeProviderToken(attemptContext.routingDecision().getProviderToken()),
                valueOrDash(failure.getMessage()),
                switchProvider ? "切换下一个服务商" : "结束转发"
        );
        return ForwardAttemptResult.retryableFailure(
                failure,
                switchProvider,
                attemptContext.routingDecision().getProvider() == null
                        ? null
                        : attemptContext.routingDecision().getProvider().getId()
        );
    }

    private void releaseAttemptResources(ForwardingDecision decision, ForwardAttemptContext attemptContext) {
        providerConcurrencyGuard.cancelAutoRenew(attemptContext.renewFuture());
        providerConcurrencyGuard.releaseQuietly(
                decision.getRequestId(),
                attemptContext.routingDecision().getProvider(),
                attemptContext.routingDecision().getProviderToken(),
                attemptContext.providerPermitLease()
        );
    }

    private Object translateFinalRetryableFailure(ForwardingDecision decision,
                                                 UpstreamRetryableException exception) {
        if (exception instanceof UpstreamTimeoutException) {
            throw recordAndReturn(decision, new BusinessException(ErrorCode.UPSTREAM_TIMEOUT, "上游请求超时"));
        }
        if (exception.getResponseEntity() != null) {
            ResponseEntity<String> responseEntity = exception.getResponseEntity();
            recordFinalFailure(
                    decision,
                    String.valueOf(responseEntity.getStatusCode().value()),
                    responseEntity.getBody()
            );
            return responseEntity;
        }
        throw recordAndReturn(decision, new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "服务商转发失败"));
    }

    private BusinessException recordAndReturn(ForwardingDecision decision, BusinessException exception) {
        recordFinalFailure(decision, String.valueOf(exception.getCode()), exception.getMessage());
        return exception;
    }

    private void recordFinalFailure(ForwardingDecision decision,
                                    String errorCode,
                                    String errorMessage) {
        ensureUsageContext(decision);
        forwardingUsageRecordService.recordErrorAsync(
                decision == null ? null : decision.getUsageContext(),
                errorCode,
                errorMessage,
                elapsedSessionMs(decision),
                elapsedSessionMs(decision),
                "error"
        );
    }

    private void ensureUsageContext(ForwardingDecision decision) {
        if (decision == null || decision.getUsageContext() != null) {
            return;
        }
        decision.setUsageContext(buildUsageContext(decision, decision.getSelectedRoute()));
    }

    private Object wrapStreamingResponse(ForwardingDecision decision,
                                         Object response,
                                         ForwardAttemptContext attemptContext) {
        ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
        ProviderStreamHandle streamHandle = (ProviderStreamHandle) responseEntity.getBody();
        PrefetchedStreamChunk prefetchedChunk = prefetchFirstStreamChunk(decision, streamHandle);
        return ResponseEntity.status(responseEntity.getStatusCode())
                .headers(responseEntity.getHeaders())
                .body(new ForwardingAsyncStreamBody(
                        streamHandle,
                        prefetchedChunk.bytes(),
                        prefetchedChunk.length(),
                        STREAM_TAIL_CAPTURE_BYTES,
                        payload -> recordStreamSuccess(
                                decision,
                                payload,
                                prefetchedChunk.firstResponseMs(),
                                elapsedSessionMs(decision)
                        ),
                        errorMessage -> recordStreamError(
                                decision,
                                errorMessage,
                                prefetchedChunk.firstResponseMs(),
                                elapsedSessionMs(decision)
                        ),
                        () -> releaseAttemptResources(decision, attemptContext)
                ));
    }

    private boolean isStreamingResponse(Object response) {
        return response instanceof ResponseEntity<?> responseEntity
                && responseEntity.getBody() instanceof ProviderStreamHandle;
    }

    /**
     * 只有成功读到首个数据块后，才允许把 SSE 会话交给异步透传层。
     * 这样首包前超时/异常仍然可以回到主重试流程执行降级。
     */
    private PrefetchedStreamChunk prefetchFirstStreamChunk(ForwardingDecision decision,
                                                           ProviderStreamHandle streamHandle) {
        byte[] buffer = new byte[STREAM_PREFETCH_CHUNK_BYTES];
        try {
            while (true) {
                int read = streamHandle.read(buffer);
                if (read == 0) {
                    continue;
                }
                if (read < 0) {
                    streamHandle.closeQuietly();
                    throw new UpstreamRetryableException("流式首包为空");
                }
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                return new PrefetchedStreamChunk(chunk, read, elapsedSessionMs(decision));
            }
        } catch (InterruptedIOException ex) {
            streamHandle.closeQuietly();
            RequestChainLogCollector.record(
                    RequestChainLogType.UPSTREAM_STREAM_TIMEOUT,
                    describeProvider(decision.getSelectedRoute().getProvider())
            );
            throw new UpstreamTimeoutException("流式首包读取超时", ex);
        } catch (java.io.IOException ex) {
            streamHandle.closeQuietly();
            RequestChainLogCollector.record(
                    RequestChainLogType.UPSTREAM_STREAM_ERROR,
                    describeProvider(decision.getSelectedRoute().getProvider()),
                    valueOrDash(ex.getMessage())
            );
            throw new UpstreamRetryableException("流式首包读取失败", ex);
        } catch (RuntimeException ex) {
            streamHandle.closeQuietly();
            throw ex;
        }
    }

    private void recordDirectResponse(ForwardingDecision decision, Object response) {
        if (!(response instanceof ResponseEntity<?> responseEntity)) {
            return;
        }
        long sessionMs = elapsedSessionMs(decision);
        Object body = responseEntity.getBody();
        String payload = body == null ? null : String.valueOf(body);
        RequestChainLogCollector.markResponseStatus(responseEntity.getStatusCode().value());
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            UsageMetrics usageMetrics = usageExtractor.extract(payload, decision.getUsageContext().getModelProvider());
            forwardingUsageRecordService.recordSuccessAsync(decision.getUsageContext(), usageMetrics, sessionMs, sessionMs);
            RequestChainLogCollector.record(RequestChainLogType.DIRECT_RESPONSE_SUCCESS, responseEntity.getStatusCode().value());
            RequestChainLogCollector.markSuccess("请求处理成功");
            return;
        }
        forwardingUsageRecordService.recordErrorAsync(
                decision.getUsageContext(),
                String.valueOf(responseEntity.getStatusCode().value()),
                payload,
                sessionMs,
                sessionMs,
                "error"
        );
        RequestChainLogCollector.record(RequestChainLogType.DIRECT_RESPONSE_ERROR, responseEntity.getStatusCode().value());
        RequestChainLogCollector.markBusinessFailure("请求处理失败");
    }

    private void recordStreamSuccess(ForwardingDecision decision,
                                     String payload,
                                     long responseMs,
                                     long sessionMs) {
        UsageMetrics usageMetrics = usageExtractor.extract(payload, decision.getUsageContext().getModelProvider());
        forwardingUsageRecordService.recordSuccessAsync(decision.getUsageContext(), usageMetrics, responseMs, sessionMs);
        RequestChainLogCollector.record(RequestChainLogType.STREAMING_RESPONSE_SUCCESS);
        RequestChainLogCollector.markSuccess("请求处理成功");
    }

    private void recordStreamError(ForwardingDecision decision,
                                   String errorMessage,
                                   long responseMs,
                                   long sessionMs) {
        forwardingUsageRecordService.recordErrorAsync(
                decision.getUsageContext(),
                "STREAM_WRITE_ERROR",
                errorMessage,
                responseMs,
                sessionMs,
                "error"
        );
        RequestChainLogCollector.record(RequestChainLogType.STREAMING_RESPONSE_ERROR, valueOrDash(errorMessage));
        RequestChainLogCollector.markUnexpectedFailure("流式响应写出失败", null);
    }

    private long elapsedSessionMs(ForwardingDecision decision) {
        Long startedAtMs = decision.getSessionStartedAtMs();
        if (startedAtMs == null) {
            return 0L;
        }
        return Math.max(System.currentTimeMillis() - startedAtMs, 0L);
    }

    private ForwardingUsageContext buildUsageContext(ForwardingDecision decision, RoutingDecision selectedRoute) {
        Provider provider = selectedRoute == null ? null : selectedRoute.getProvider();
        ProviderToken providerToken = selectedRoute == null ? null : selectedRoute.getProviderToken();
        return ForwardingUsageContext.builder()
                .requestId(decision.getRequestId())
                .accountId(decision.getCustomerAccount() == null ? null : decision.getCustomerAccount().getId())
                .customerTokenId(decision.getCustomerToken() == null ? null : decision.getCustomerToken().getId())
                .customerTokenValue(decision.getRequest() == null ? null : decision.getRequest().getCustomerToken())
                .planId(decision.getCustomerPlan() == null ? null : decision.getCustomerPlan().getId())
                .providerId(provider == null ? null : provider.getId())
                .providerCode(provider == null ? null : provider.getProviderCode())
                .providerName(provider == null ? null : provider.getProviderName())
                .providerTokenId(providerToken == null ? null : providerToken.getId())
                .providerTokenName(providerToken == null ? null : providerToken.getTokenName())
                .providerTokenValue(providerToken == null ? null : providerToken.getTokenValue())
                .endpointCode(decision.getRequest() == null || decision.getRequest().getProtocol() == null
                        ? null
                        : decision.getRequest().getProtocol().getCode())
                .modelId(decision.getModel() == null ? null : decision.getModel().getId())
                .modelCode(decision.getModel() == null ? null : decision.getModel().getModelCode())
                .modelName(decision.getModel() == null ? null : decision.getModel().getModelName())
                .modelProvider(decision.getModel() == null ? null : decision.getModel().getModelProvider())
                .inputPrice(decision.getModel() == null ? null : decision.getModel().getInputPrice())
                .cacheHitPrice(decision.getModel() == null ? null : decision.getModel().getCacheHitPrice())
                .outputPrice(decision.getModel() == null ? null : decision.getModel().getOutputPrice())
                .multiplier(decision.getCustomerPlan() == null ? null : decision.getCustomerPlan().getMultiplier())
                .build();
    }

    private void recordAttemptStarted(int attempt,
                                      Provider provider,
                                      ProviderToken providerToken,
                                      ProviderPermitLease permitLease) {
        ProviderRuntimePolicy runtimePolicy = permitLease == null ? null : permitLease.runtimePolicy();
        RequestChainLogCollector.record(
                RequestChainLogType.FORWARD_ATTEMPT_STARTED,
                attempt,
                describeProvider(provider),
                describeProviderToken(providerToken),
                maskTokenValue(providerToken == null ? null : providerToken.getTokenValue()),
                describeConcurrencyLimit(runtimePolicy),
                valueOrDash(runtimePolicy == null ? null : runtimePolicy.acquireTimeoutMs()),
                valueOrDash(runtimePolicy == null ? null : runtimePolicy.requestTimeoutMs()),
                valueOrDash(runtimePolicy == null ? null : runtimePolicy.streamFirstResponseTimeoutMs()),
                valueOrDash(runtimePolicy == null ? null : runtimePolicy.streamIdleTimeoutMs()),
                valueOrDash(runtimePolicy == null ? null : runtimePolicy.sessionLeaseMs()),
                valueOrDash(runtimePolicy == null ? null : runtimePolicy.sessionRenewIntervalMs())
        );
    }

    private String describeConcurrencyLimit(ProviderRuntimePolicy runtimePolicy) {
        if (runtimePolicy == null) {
            return "-";
        }
        if (!runtimePolicy.concurrencyLimitEnabled()) {
            return "未开启";
        }
        return String.valueOf(runtimePolicy.maxConcurrentPerToken());
    }

    private String describeProvider(Provider provider) {
        if (provider == null) {
            return "-";
        }
        return valueOrDash(provider.getProviderName())
                + "/" + valueOrDash(provider.getProviderCode())
                + "(" + valueOrDash(provider.getId()) + ")";
    }

    private String describeProviderToken(ProviderToken providerToken) {
        if (providerToken == null) {
            return "-";
        }
        return valueOrDash(providerToken.getTokenName()) + "(" + valueOrDash(providerToken.getId()) + ")";
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

    private record ForwardAttemptResult(Object response,
                                        UpstreamRetryableException retryableFailure,
                                        boolean switchProvider,
                                        Long providerId) {

        private static ForwardAttemptResult success(Object response) {
            return new ForwardAttemptResult(response, null, false, null);
        }

        private static ForwardAttemptResult retryableFailure(UpstreamRetryableException failure,
                                                             boolean switchProvider,
                                                             Long providerId) {
            return new ForwardAttemptResult(null, failure, switchProvider, providerId);
        }
    }

    private record ProviderTokenSelectionResult(ProviderToken providerToken,
                                                ProviderPermitLease providerPermitLease) {

        private ProviderTokenSelectionResult(ProviderToken providerToken) {
            this(providerToken, null);
        }
    }

    private static final class ForwardAttemptContext {

        private final RoutingDecision routingDecision;
        private final ProviderPermitLease providerPermitLease;
        private ScheduledFuture<?> renewFuture;

        private ForwardAttemptContext(RoutingDecision routingDecision,
                                      ProviderPermitLease providerPermitLease,
                                      ScheduledFuture<?> renewFuture) {
            this.routingDecision = routingDecision;
            this.providerPermitLease = providerPermitLease;
            this.renewFuture = renewFuture;
        }

        private RoutingDecision routingDecision() {
            return routingDecision;
        }

        private ProviderPermitLease providerPermitLease() {
            return providerPermitLease;
        }

        private ScheduledFuture<?> renewFuture() {
            return renewFuture;
        }
    }

    private record PrefetchedStreamChunk(byte[] bytes, int length, long firstResponseMs) {
    }
}
