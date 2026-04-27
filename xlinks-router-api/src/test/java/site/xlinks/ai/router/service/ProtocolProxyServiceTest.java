package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapter;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapterFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.service.routing.ProxyRoutingPipeline;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;
import site.xlinks.ai.router.service.routing.RoutingStepException;

import java.util.Set;

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
}
