package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapter;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapterFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.routing.ProxyRoutingPipeline;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;
import site.xlinks.ai.router.service.routing.RoutingStepException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtocolProxyServiceTest {

    @Test
    void shouldRecordRoutingFailureWhenProviderSelectionFailsAfterTokenValidation() {
        CustomerTokenAuthService customerTokenAuthService = mock(CustomerTokenAuthService.class);
        ProviderProtocolAdapterFactory adapterFactory = mock(ProviderProtocolAdapterFactory.class);
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        UsageRecordService usageRecordService = mock(UsageRecordService.class);
        UsageExtractor usageExtractor = mock(UsageExtractor.class);
        ProxyRoutingPipeline proxyRoutingPipeline = mock(ProxyRoutingPipeline.class);
        ProviderConcurrencyGuard providerConcurrencyGuard = mock(ProviderConcurrencyGuard.class);
        TaskExecutor sseTaskExecutor = mock(TaskExecutor.class);
        TaskExecutor sseWriterTaskExecutor = mock(TaskExecutor.class);
        ProviderProtocolAdapter adapter = mock(ProviderProtocolAdapter.class);

        ProtocolProxyService service = new ProtocolProxyService(
                customerTokenAuthService,
                adapterFactory,
                routeCacheService,
                usageRecordService,
                usageExtractor,
                proxyRoutingPipeline,
                providerConcurrencyGuard,
                sseTaskExecutor,
                sseWriterTaskExecutor
        );

        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .model("gpt-x")
                .build();
        when(adapterFactory.getAdapter(ProxyProtocol.RESPONSES)).thenReturn(adapter);

        RoutingBuildContext routingContext = new RoutingBuildContext("sk-user", request, "req_route_fail", Set.of(), Set.of());
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(501L);
        customerToken.setAccountId(100L);
        customerToken.setCustomerName("merchant-a");
        customerToken.setTokenName("token-a");
        routingContext.setCustomerToken(customerToken);
        routingContext.setUsageDecision(UsageDecision.builder()
                .customerTokenId(501L)
                .customerName("merchant-a")
                .balanceEnabled(true)
                .build());

        when(proxyRoutingPipeline.resolve(anyString(), any(ProxyRequest.class), anyString(), any(), any()))
                .thenThrow(new RoutingStepException(
                        routingContext,
                        site.xlinks.ai.router.service.routing.ProxyErrors.noProviderToken("gpt-x")
                ));

        RoutingStepException exception = assertThrows(
                RoutingStepException.class,
                () -> service.forwardDirect("sk-user", request)
        );

        assertEquals(ErrorCode.ROUTE_ERROR.getCode(), exception.getCode());
        verify(usageRecordService).recordRoutingError(
                any(RoutingBuildContext.class),
                org.mockito.ArgumentMatchers.eq(500),
                org.mockito.ArgumentMatchers.eq(String.valueOf(ErrorCode.ROUTE_ERROR.getCode())),
                org.mockito.ArgumentMatchers.eq("No available provider token for model and protocol: gpt-x"),
                anyLong(),
                org.mockito.ArgumentMatchers.eq("provider_token_unavailable")
        );
        verify(usageRecordService, never()).recordError(any(), anyInt(), anyString(), anyString(), anyLong(), anyString());
        verify(adapter, never()).forwardDirect(any(), any());
    }

    @Test
    void shouldNotMarkRouteFailureWhenStreamWriterFailsLocally() {
        CustomerTokenAuthService customerTokenAuthService = mock(CustomerTokenAuthService.class);
        ProviderProtocolAdapterFactory adapterFactory = mock(ProviderProtocolAdapterFactory.class);
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        UsageRecordService usageRecordService = mock(UsageRecordService.class);
        UsageExtractor usageExtractor = mock(UsageExtractor.class);
        ProxyRoutingPipeline proxyRoutingPipeline = mock(ProxyRoutingPipeline.class);
        ProviderConcurrencyGuard providerConcurrencyGuard = mock(ProviderConcurrencyGuard.class);
        TaskExecutor sseTaskExecutor = mock(TaskExecutor.class);
        TaskExecutor sseWriterTaskExecutor = task -> {
            Thread worker = new Thread(task, "test-sse-writer");
            worker.setDaemon(true);
            worker.start();
        };
        ProviderProtocolAdapter adapter = mock(ProviderProtocolAdapter.class);

        ProtocolProxyService service = new ProtocolProxyService(
                customerTokenAuthService,
                adapterFactory,
                routeCacheService,
                usageRecordService,
                usageExtractor,
                proxyRoutingPipeline,
                providerConcurrencyGuard,
                sseTaskExecutor,
                sseWriterTaskExecutor
        );

        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.RESPONSES)
                .model("gpt-x")
                .stream(true)
                .build();
        when(adapterFactory.getAdapter(ProxyProtocol.RESPONSES)).thenReturn(adapter);

        RoutingBuildContext routingContext = new RoutingBuildContext("sk-user", request, "req_stream_fail", Set.of(), Set.of());
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(501L);
        customerToken.setAccountId(100L);
        customerToken.setCustomerName("merchant-a");
        customerToken.setTokenName("token-a");
        routingContext.setCustomerToken(customerToken);
        routingContext.setUsageDecision(UsageDecision.builder()
                .customerTokenId(501L)
                .customerName("merchant-a")
                .balanceEnabled(true)
                .build());
        Model model = new Model();
        model.setId(11L);
        model.setModelCode("gpt-x");
        model.setModelName("GPT X");
        model.setModelProvider("OPENAI");
        routingContext.setModel(model);

        Provider provider = new Provider();
        provider.setId(10L);
        provider.setProviderCode("p1");
        provider.setProviderName("provider-1");
        provider.setBaseUrl("https://example.com");
        routingContext.setProvider(provider);

        ProviderModel providerModel = new ProviderModel();
        providerModel.setId(12L);
        providerModel.setProviderId(10L);
        providerModel.setProviderModelCode("pm1");
        providerModel.setProviderModelName("Provider Model 1");
        routingContext.setProviderModel(providerModel);

        ProviderToken providerToken = new ProviderToken();
        providerToken.setId(20L);
        providerToken.setTokenValue("provider-token");
        providerToken.setTokenName("provider-token-1");
        routingContext.setProviderToken(providerToken);

        when(proxyRoutingPipeline.resolve(anyString(), any(ProxyRequest.class), anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    routingContext.setProviderPermitLease(null);
                    return routingContext;
                });

        org.springframework.test.util.ReflectionTestUtils.setField(service, "streamDispatchJoinTimeoutMs", 1000L);

        org.mockito.Mockito.doAnswer(invocation -> {
            Consumer<StreamEvent> onEvent = invocation.getArgument(2);
            onEvent.accept(StreamEvent.builder().dataLine("{\"type\":\"response.output_text.delta\"}").build());
            return null;
        }).when(adapter).forwardStream(any(), any(), any(Consumer.class), any(AtomicBoolean.class));

        when(usageExtractor.extract(any(StreamEvent.class), anyString())).thenReturn(null);

        assertThrows(
                site.xlinks.ai.router.common.exception.BusinessException.class,
                () -> service.forwardStream("sk-user", request, event -> {
                    throw new RuntimeException("downstream write failed");
                }, new AtomicBoolean(false))
        );

        verify(routeCacheService, never()).recordProviderFailure(any());
        verify(routeCacheService, never()).recordProviderTokenFailure(any());
        verify(usageRecordService, never()).recordError(any(), anyInt(), anyString(), anyString(), anyLong(), anyString());
    }
}
