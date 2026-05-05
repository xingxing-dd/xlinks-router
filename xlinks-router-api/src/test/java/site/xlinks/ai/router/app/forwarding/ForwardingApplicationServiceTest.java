package site.xlinks.ai.router.app.forwarding;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.model.ForwardingAsyncStreamBody;
import site.xlinks.ai.router.app.forwarding.model.ForwardingConsumptionMode;
import site.xlinks.ai.router.app.forwarding.model.ForwardingUsageContext;
import site.xlinks.ai.router.app.forwarding.model.ProviderPermitLease;
import site.xlinks.ai.router.app.forwarding.model.ProviderRuntimePolicy;
import site.xlinks.ai.router.app.forwarding.model.UsageMetrics;
import site.xlinks.ai.router.app.forwarding.retry.DefaultForwardFailureRoutingStrategy;
import site.xlinks.ai.router.domain.provider.ProviderTokenSelectionService;
import site.xlinks.ai.router.domain.routing.DefaultRoutingDomainService;
import site.xlinks.ai.router.domain.routing.model.RoutingDecisionStage;
import site.xlinks.ai.router.domain.routing.model.RoutingPlan;
import site.xlinks.ai.router.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.infrastructure.http.ProviderHttpForwardingExecutor;
import site.xlinks.ai.router.infrastructure.http.exception.UpstreamRetryableException;
import site.xlinks.ai.router.infrastructure.http.model.ProviderStreamHandle;
import site.xlinks.ai.router.infrastructure.runtime.ProviderConcurrencyGuard;
import site.xlinks.ai.router.infrastructure.runtime.ProviderRuntimeStateService;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ForwardingApplicationServiceTest {

    private ForwardingReadModelLoader forwardingReadModelLoader;
    private DefaultRoutingDomainService routingDomainService;
    private ProviderHttpForwardingExecutor providerHttpForwardingExecutor;
    private ProviderConcurrencyGuard providerConcurrencyGuard;
    private ProviderRuntimeStateService providerRuntimeStateService;
    private ForwardingUsageRecordService forwardingUsageRecordService;
    private UsageExtractor usageExtractor;
    private ProviderTokenSelectionService providerTokenSelectionService;
    private DistributedRouteCacheRepository distributedRouteCacheRepository;
    private ForwardingApplicationService forwardingApplicationService;

    @BeforeEach
    void setUp() {
        forwardingReadModelLoader = mock(ForwardingReadModelLoader.class);
        routingDomainService = mock(DefaultRoutingDomainService.class);
        providerHttpForwardingExecutor = mock(ProviderHttpForwardingExecutor.class);
        providerConcurrencyGuard = mock(ProviderConcurrencyGuard.class);
        providerRuntimeStateService = mock(ProviderRuntimeStateService.class);
        forwardingUsageRecordService = mock(ForwardingUsageRecordService.class);
        usageExtractor = mock(UsageExtractor.class);
        providerTokenSelectionService = mock(ProviderTokenSelectionService.class);
        distributedRouteCacheRepository = mock(DistributedRouteCacheRepository.class);

        ForwardingDecisionService forwardingDecisionService = new ForwardingDecisionService(routingDomainService);
        ForwardingExecutionService forwardingExecutionService = new ForwardingExecutionService(
                forwardingReadModelLoader,
                providerHttpForwardingExecutor,
                providerConcurrencyGuard,
                new DefaultForwardFailureRoutingStrategy(),
                providerRuntimeStateService,
                forwardingUsageRecordService,
                usageExtractor,
                providerTokenSelectionService,
                distributedRouteCacheRepository
        );
        ReflectionTestUtils.setField(forwardingExecutionService, "maxRetryAttempts", 3);
        forwardingApplicationService = new ForwardingApplicationService(
                forwardingDecisionService,
                forwardingExecutionService
        );
    }

    @Test
    void shouldSwitchToNextProviderWhenRetryableFailureOccurs() {
        ForwardRequest request = buildRequest(false);
        Provider firstProvider = provider(1001L);
        Provider secondProvider = provider(1002L);
        ProviderToken firstToken = providerToken(2001L, 1001L);
        ProviderToken secondToken = providerToken(2002L, 1002L);
        ProviderPermitLease firstLease = permitLease(1001L, 2001L, null);
        ProviderPermitLease secondLease = permitLease(1002L, 2002L, null);

        when(routingDomainService.route(any()))
                .thenReturn(routingPlan(request, 1001L, 1002L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(firstProvider);
        when(forwardingReadModelLoader.loadProvider(1002L)).thenReturn(secondProvider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(firstToken));
        when(forwardingReadModelLoader.loadProviderTokens(1002L)).thenReturn(List.of(secondToken));
        when(providerTokenSelectionService.select(eq(firstProvider), anyList())).thenReturn(firstToken);
        when(providerTokenSelectionService.select(eq(secondProvider), anyList())).thenReturn(secondToken);
        when(providerConcurrencyGuard.tryAcquire(eq(firstProvider), eq(firstToken), any())).thenReturn(firstLease);
        when(providerConcurrencyGuard.tryAcquire(eq(secondProvider), eq(secondToken), any())).thenReturn(secondLease);
        when(providerHttpForwardingExecutor.forward(any()))
                .thenThrow(new UpstreamRetryableException("retry"))
                .thenReturn(ResponseEntity.ok("success"));
        when(usageExtractor.extract("success", "OPENAI"))
                .thenReturn(UsageMetrics.builder().inputTokens(1).outputTokens(1).totalTokens(2).build());

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        assertEquals("success", responseEntity.getBody());
        verify(providerRuntimeStateService).recordFailure(argThat(route ->
                route != null && route.getProvider() != null && route.getProvider().getId().equals(1001L)));
        verify(providerRuntimeStateService).clearFailure(argThat(route ->
                route != null && route.getProvider() != null && route.getProvider().getId().equals(1002L)));
        verify(forwardingUsageRecordService).recordSuccessAsync(any(ForwardingUsageContext.class), any(UsageMetrics.class), anyLong(), anyLong());
        verify(providerConcurrencyGuard, times(2)).releaseQuietly(any(), any(), any(), any());
    }

    @Test
    void shouldTryNextTokenWithinSameProviderWhenPermitAcquireFails() {
        ForwardRequest request = buildRequest(false);
        Provider provider = provider(1001L);
        ProviderToken firstToken = providerToken(2001L, 1001L);
        ProviderToken secondToken = providerToken(2002L, 1001L);
        ProviderPermitLease secondLease = permitLease(1001L, 2002L, null);

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(firstToken, secondToken));
        when(providerTokenSelectionService.select(eq(provider), anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<ProviderToken> tokens = invocation.getArgument(1);
                    return tokens.isEmpty() ? null : tokens.get(0);
                });
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(firstToken), any())).thenReturn(null);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(secondToken), any())).thenReturn(secondLease);
        when(providerHttpForwardingExecutor.forward(any())).thenReturn(ResponseEntity.ok("success"));
        when(usageExtractor.extract("success", "OPENAI"))
                .thenReturn(UsageMetrics.builder().inputTokens(1).outputTokens(1).totalTokens(2).build());

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        assertEquals("success", responseEntity.getBody());
        verify(providerHttpForwardingExecutor, times(1)).forward(any());
        verify(providerConcurrencyGuard).tryAcquire(eq(provider), eq(firstToken), any());
        verify(providerConcurrencyGuard).tryAcquire(eq(provider), eq(secondToken), any());
        verify(providerRuntimeStateService).clearFailure(argThat(route ->
                route != null && route.getProviderToken() != null && route.getProviderToken().getId().equals(2002L)));
    }

    @Test
    void shouldReleasePermitAfterStreamingBodyCompletes() throws Exception {
        ForwardRequest request = buildRequest(true);
        Provider provider = provider(1001L);
        ProviderToken token = providerToken(2001L, 1001L);
        ProviderPermitLease lease = permitLease(1001L, 2001L, "permit-1");
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> renewFuture = mock(ScheduledFuture.class);

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(eq(provider), anyList())).thenReturn(token);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(token), any())).thenReturn(lease);
        doAnswer(invocation -> renewFuture)
                .when(providerConcurrencyGuard)
                .scheduleAutoRenew(any(), eq(provider), eq(token), eq(lease));
        when(providerHttpForwardingExecutor.forward(any()))
                .thenReturn(ResponseEntity.ok(providerStreamHandle(new ByteArrayInputStream("stream-ok".getBytes(StandardCharsets.UTF_8)))));
        when(usageExtractor.extract(any(String.class), any()))
                .thenReturn(UsageMetrics.builder().inputTokens(2).outputTokens(1).totalTokens(3).build());

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        ForwardingAsyncStreamBody body = assertInstanceOf(ForwardingAsyncStreamBody.class, responseEntity.getBody());
        assertEquals("stream-ok", readAll(body));
        body.completeSuccess();

        verify(providerConcurrencyGuard).cancelAutoRenew(renewFuture);
        verify(providerConcurrencyGuard).releaseQuietly(any(), eq(provider), eq(token), eq(lease));
        verify(forwardingUsageRecordService).recordSuccessAsync(any(ForwardingUsageContext.class), any(UsageMetrics.class), anyLong(), anyLong());
    }

    @Test
    void shouldReleasePermitWhenAutoRenewSchedulingFails() {
        ForwardRequest request = buildRequest(false);
        Provider provider = provider(1001L);
        ProviderToken token = providerToken(2001L, 1001L);
        ProviderPermitLease lease = permitLease(1001L, 2001L, "permit-1");

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(eq(provider), anyList())).thenReturn(token);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(token), any())).thenReturn(lease);
        when(providerConcurrencyGuard.scheduleAutoRenew(any(), eq(provider), eq(token), eq(lease)))
                .thenThrow(new TaskRejectedException("scheduler rejected"));

        assertThrows(TaskRejectedException.class, () -> forwardingApplicationService.forward(request));

        verify(providerConcurrencyGuard).releaseQuietly(any(), eq(provider), eq(token), eq(lease));
        verify(providerConcurrencyGuard).cancelAutoRenew(any());
        verifyNoInteractions(providerHttpForwardingExecutor);
    }

    @Test
    void shouldRetryNextProviderWhenStreamFirstChunkTimesOut() throws Exception {
        ForwardRequest request = buildRequest(true);
        Provider firstProvider = provider(1001L);
        Provider secondProvider = provider(1002L);
        ProviderToken firstToken = providerToken(2001L, 1001L);
        ProviderToken secondToken = providerToken(2002L, 1002L);
        ProviderPermitLease firstLease = permitLease(1001L, 2001L, "permit-1");
        ProviderPermitLease secondLease = permitLease(1002L, 2002L, "permit-2");

        when(routingDomainService.route(any()))
                .thenReturn(routingPlan(request, 1001L, 1002L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(firstProvider);
        when(forwardingReadModelLoader.loadProvider(1002L)).thenReturn(secondProvider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(firstToken));
        when(forwardingReadModelLoader.loadProviderTokens(1002L)).thenReturn(List.of(secondToken));
        when(providerTokenSelectionService.select(eq(firstProvider), anyList())).thenReturn(firstToken);
        when(providerTokenSelectionService.select(eq(secondProvider), anyList())).thenReturn(secondToken);
        when(providerConcurrencyGuard.tryAcquire(eq(firstProvider), eq(firstToken), any())).thenReturn(firstLease);
        when(providerConcurrencyGuard.tryAcquire(eq(secondProvider), eq(secondToken), any())).thenReturn(secondLease);
        when(providerHttpForwardingExecutor.forward(any()))
                .thenReturn(ResponseEntity.ok(providerStreamHandle(new TimeoutOnFirstReadInputStream("first chunk timeout"))))
                .thenReturn(ResponseEntity.ok(providerStreamHandle(new ByteArrayInputStream("stream-ok".getBytes(StandardCharsets.UTF_8)))));
        when(usageExtractor.extract(any(String.class), any()))
                .thenReturn(UsageMetrics.builder().inputTokens(2).outputTokens(1).totalTokens(3).build());

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        ForwardingAsyncStreamBody body = assertInstanceOf(ForwardingAsyncStreamBody.class, responseEntity.getBody());
        assertEquals("stream-ok", readAll(body));
        body.completeSuccess();

        verify(providerHttpForwardingExecutor, times(2)).forward(any());
        verify(providerRuntimeStateService).recordFailure(argThat(route ->
                route != null && route.getProvider() != null && route.getProvider().getId().equals(1001L)));
        verify(providerRuntimeStateService).clearFailure(argThat(route ->
                route != null && route.getProvider() != null && route.getProvider().getId().equals(1002L)));
        verify(providerConcurrencyGuard).releaseQuietly(any(), eq(firstProvider), eq(firstToken), eq(firstLease));
        verify(providerConcurrencyGuard).releaseQuietly(any(), eq(secondProvider), eq(secondToken), eq(secondLease));
    }

    @Test
    void shouldRecordDirectErrorWhenNon2xxResponseReturned() {
        ForwardRequest request = buildRequest(false);
        Provider provider = provider(1001L);
        ProviderToken token = providerToken(2001L, 1001L);
        ProviderPermitLease lease = permitLease(1001L, 2001L, null);
        AtomicLong recordedResponseMs = new AtomicLong(-1L);
        AtomicLong recordedSessionMs = new AtomicLong(-1L);

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(eq(provider), anyList())).thenReturn(token);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(token), any())).thenReturn(lease);
        when(providerHttpForwardingExecutor.forward(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(5L);
                    return ResponseEntity.status(429).body("{\"error\":\"rate limit\"}");
                });
        doAnswer(invocation -> {
            recordedResponseMs.set(invocation.getArgument(3, Long.class));
            recordedSessionMs.set(invocation.getArgument(4, Long.class));
            return null;
        }).when(forwardingUsageRecordService).recordErrorAsync(any(ForwardingUsageContext.class), eq("429"), any(), anyLong(), anyLong(), eq("error"));

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(429, responseEntity.getStatusCode().value());
        verify(forwardingUsageRecordService).recordErrorAsync(any(ForwardingUsageContext.class), eq("429"), eq("{\"error\":\"rate limit\"}"), anyLong(), anyLong(), eq("error"));
        verifyNoInteractions(usageExtractor);
        assertEquals(recordedResponseMs.get(), recordedSessionMs.get());
        assertTrue(recordedSessionMs.get() >= 0L);
    }

    @Test
    void shouldRecordStreamErrorWhenStreamingWriteFails() throws Exception {
        ForwardRequest request = buildRequest(true);
        Provider provider = provider(1001L);
        ProviderToken token = providerToken(2001L, 1001L);
        ProviderPermitLease lease = permitLease(1001L, 2001L, "permit-1");
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> renewFuture = mock(ScheduledFuture.class);
        AtomicLong recordedResponseMs = new AtomicLong(-1L);
        AtomicLong recordedSessionMs = new AtomicLong(-1L);

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(eq(provider), anyList())).thenReturn(token);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(token), any())).thenReturn(lease);
        doAnswer(invocation -> renewFuture)
                .when(providerConcurrencyGuard)
                .scheduleAutoRenew(any(), eq(provider), eq(token), eq(lease));
        when(providerHttpForwardingExecutor.forward(any()))
                .thenReturn(ResponseEntity.ok(providerStreamHandle(new BrokenInputStream(
                        "partial".getBytes(StandardCharsets.UTF_8),
                        "stream broken"
                ))));
        doAnswer(invocation -> {
            recordedResponseMs.set(invocation.getArgument(3, Long.class));
            recordedSessionMs.set(invocation.getArgument(4, Long.class));
            return null;
        }).when(forwardingUsageRecordService).recordErrorAsync(any(ForwardingUsageContext.class), eq("STREAM_WRITE_ERROR"), eq("stream broken"), anyLong(), anyLong(), eq("error"));

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        ForwardingAsyncStreamBody body = assertInstanceOf(ForwardingAsyncStreamBody.class, responseEntity.getBody());
        byte[] buffer = new byte[16];
        assertEquals("partial", readChunk(body, buffer));
        IOException exception = assertThrows(IOException.class, () -> body.read(buffer));
        body.completeError(exception.getMessage());

        assertEquals("stream broken", exception.getMessage());
        verify(forwardingUsageRecordService).recordErrorAsync(any(ForwardingUsageContext.class), eq("STREAM_WRITE_ERROR"), eq("stream broken"), anyLong(), anyLong(), eq("error"));
        verify(providerConcurrencyGuard).cancelAutoRenew(renewFuture);
        verify(providerConcurrencyGuard).releaseQuietly(any(), eq(provider), eq(token), eq(lease));
        assertTrue(recordedResponseMs.get() >= 0L);
        assertTrue(recordedSessionMs.get() >= recordedResponseMs.get());
        assertTrue(recordedSessionMs.get() >= 0L);
    }

    @Test
    void shouldRecordFirstResponseMsSeparatelyForStreamingSuccess() throws Exception {
        ForwardRequest request = buildRequest(true);
        Provider provider = provider(1001L);
        ProviderToken token = providerToken(2001L, 1001L);
        ProviderPermitLease lease = permitLease(1001L, 2001L, "permit-1");
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> renewFuture = mock(ScheduledFuture.class);
        AtomicLong recordedResponseMs = new AtomicLong(-1L);
        AtomicLong recordedSessionMs = new AtomicLong(-1L);
        AtomicReference<UsageMetrics> recordedUsageMetrics = new AtomicReference<>();

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(eq(provider), anyList())).thenReturn(token);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(token), any())).thenReturn(lease);
        doAnswer(invocation -> renewFuture)
                .when(providerConcurrencyGuard)
                .scheduleAutoRenew(any(), eq(provider), eq(token), eq(lease));
        when(providerHttpForwardingExecutor.forward(any()))
                .thenReturn(ResponseEntity.ok(providerStreamHandle(new DelayedChunkInputStream(
                        "first".getBytes(StandardCharsets.UTF_8),
                        " second".getBytes(StandardCharsets.UTF_8),
                        20L
                ))));
        UsageMetrics usageMetrics = UsageMetrics.builder().inputTokens(2).outputTokens(1).totalTokens(3).build();
        when(usageExtractor.extract(any(String.class), any())).thenReturn(usageMetrics);
        doAnswer(invocation -> {
            recordedUsageMetrics.set(invocation.getArgument(1, UsageMetrics.class));
            recordedResponseMs.set(invocation.getArgument(2, Long.class));
            recordedSessionMs.set(invocation.getArgument(3, Long.class));
            return null;
        }).when(forwardingUsageRecordService).recordSuccessAsync(any(ForwardingUsageContext.class), any(UsageMetrics.class), anyLong(), anyLong());

        Object result = forwardingApplicationService.forward(request);

        ResponseEntity<?> responseEntity = assertInstanceOf(ResponseEntity.class, result);
        ForwardingAsyncStreamBody body = assertInstanceOf(ForwardingAsyncStreamBody.class, responseEntity.getBody());
        assertEquals("first second", readAll(body));
        Thread.sleep(30L);
        body.completeSuccess();

        assertEquals(usageMetrics, recordedUsageMetrics.get());
        assertTrue(recordedResponseMs.get() >= 0L);
        assertTrue(recordedSessionMs.get() > recordedResponseMs.get());
    }

    @Test
    void shouldThrowWhenRetriesExhaustedWithoutSuccessfulRoute() {
        ForwardRequest request = buildRequest(false);
        Provider provider = provider(1001L);
        ProviderToken token = providerToken(2001L, 1001L);
        ProviderPermitLease lease = permitLease(1001L, 2001L, null);

        when(routingDomainService.route(any())).thenReturn(routingPlan(request, 1001L));
        when(forwardingReadModelLoader.loadProvider(1001L)).thenReturn(provider);
        when(forwardingReadModelLoader.loadProviderTokens(1001L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(eq(provider), anyList())).thenReturn(token);
        when(providerConcurrencyGuard.tryAcquire(eq(provider), eq(token), any())).thenReturn(lease);
        when(providerHttpForwardingExecutor.forward(any()))
                .thenThrow(new UpstreamRetryableException("retry"));

        BusinessException exception = assertThrows(BusinessException.class, () -> forwardingApplicationService.forward(request));

        assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode(), exception.getCode());
        verify(providerRuntimeStateService, times(1)).recordFailure(any());
    }

    private ForwardRequest buildRequest(boolean stream) {
        return ForwardRequest.builder()
                .protocol(ForwardProtocol.CHAT_COMPLETIONS)
                .model("gpt-4o-mini")
                .customerToken("customer-token")
                .requestBody("{\"model\":\"gpt-4o-mini\"}")
                .stream(stream)
                .build();
    }

    private RoutingPlan routingPlan(ForwardRequest request, Long... providerIds) {
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(101L);
        customerToken.setAccountId(201L);
        customerToken.setTokenName("测试客户令牌");

        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(201L);
        customerAccount.setUsername("tester@example.com");

        CustomerPlan customerPlan = new CustomerPlan();
        customerPlan.setId(301L);
        customerPlan.setPlanId(401L);
        customerPlan.setPlanName("测试套餐");
        customerPlan.setMultiplier(BigDecimal.ONE);

        Model model = new Model();
        model.setId(501L);
        model.setModelCode(request.getModel());
        model.setModelName("测试模型");
        model.setModelProvider("OPENAI");

        List<ProviderModel> providerModels = java.util.Arrays.stream(providerIds)
                .map(providerId -> {
                    ProviderModel providerModel = new ProviderModel();
                    providerModel.setId(providerId + 10000);
                    providerModel.setProviderId(providerId);
                    providerModel.setModelId(501L);
                    providerModel.setProviderModelCode("upstream-model-" + providerId);
                    return providerModel;
                })
                .toList();
        return RoutingPlan.builder()
                .customerToken(customerToken)
                .customerAccount(customerAccount)
                .customerPlan(customerPlan)
                .model(model)
                .consumptionMode(ForwardingConsumptionMode.PLAN)
                .accountId(201L)
                .modelId(501L)
                .orderedProviderModels(providerModels)
                .candidateProviderModels(providerModels)
                .decisionStage(RoutingDecisionStage.ROUTED)
                .build();
    }

    private Provider provider(Long providerId) {
        Provider provider = new Provider();
        provider.setId(providerId);
        provider.setProviderCode("provider-" + providerId);
        provider.setProviderName("provider-" + providerId);
        provider.setBaseUrl("https://example.com");
        provider.setStatus(1);
        provider.setRequestTimeoutMs(1000);
        provider.setStreamFirstResponseTimeoutMs(1000);
        provider.setStreamIdleTimeoutMs(1000);
        return provider;
    }

    private ProviderToken providerToken(Long tokenId, Long providerId) {
        ProviderToken providerToken = new ProviderToken();
        providerToken.setId(tokenId);
        providerToken.setProviderId(providerId);
        providerToken.setTokenStatus(1);
        providerToken.setTokenName("token-" + tokenId);
        providerToken.setTokenValue("provider-token-" + tokenId);
        return providerToken;
    }

    private ProviderPermitLease permitLease(Long providerId, Long providerTokenId, String permitId) {
        return new ProviderPermitLease(
                providerId,
                providerTokenId,
                "token-" + providerTokenId,
                permitId,
                new ProviderRuntimePolicy(permitId != null, 2, 100, 1000, 1000, 1000, 5000, 1000)
        );
    }

    private ProviderStreamHandle providerStreamHandle(InputStream inputStream) {
        Request request = new Request.Builder().url("https://example.com/v1/chat/completions").build();
        ResponseBody responseBody = ResponseBody.create("", MediaType.get("text/event-stream"));
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody)
                .build();
        return new ProviderStreamHandle(response, responseBody, inputStream);
    }

    private String readAll(ForwardingAsyncStreamBody body) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8];
        int read;
        while ((read = body.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private String readChunk(ForwardingAsyncStreamBody body, byte[] buffer) throws IOException {
        int read = body.read(buffer);
        if (read <= 0) {
            return "";
        }
        return new String(buffer, 0, read, StandardCharsets.UTF_8);
    }

    private static final class BrokenInputStream extends InputStream {

        private final byte[] firstChunk;
        private final String errorMessage;
        private boolean firstReadCompleted;

        private BrokenInputStream(byte[] firstChunk, String errorMessage) {
            this.firstChunk = firstChunk;
            this.errorMessage = errorMessage;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!firstReadCompleted) {
                firstReadCompleted = true;
                System.arraycopy(firstChunk, 0, b, off, firstChunk.length);
                return firstChunk.length;
            }
            throw new IOException(errorMessage);
        }

        @Override
        public int read() throws IOException {
            throw new IOException("not supported");
        }
    }

    private static final class TimeoutOnFirstReadInputStream extends InputStream {

        private final String errorMessage;
        private boolean firstRead;

        private TimeoutOnFirstReadInputStream(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!firstRead) {
                firstRead = true;
                throw new InterruptedIOException(errorMessage);
            }
            return -1;
        }

        @Override
        public int read() throws IOException {
            throw new IOException("not supported");
        }
    }

    private static final class DelayedChunkInputStream extends InputStream {

        private final byte[] firstChunk;
        private final byte[] secondChunk;
        private final long delayMs;
        private int index;

        private DelayedChunkInputStream(byte[] firstChunk, byte[] secondChunk, long delayMs) {
            this.firstChunk = firstChunk;
            this.secondChunk = secondChunk;
            this.delayMs = delayMs;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                if (index == 0) {
                    index++;
                    System.arraycopy(firstChunk, 0, b, off, firstChunk.length);
                    return firstChunk.length;
                }
                if (index == 1) {
                    Thread.sleep(delayMs);
                    index++;
                    System.arraycopy(secondChunk, 0, b, off, secondChunk.length);
                    return secondChunk.length;
                }
                return -1;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted", ex);
            }
        }

        @Override
        public int read() throws IOException {
            throw new IOException("not supported");
        }
    }
}
