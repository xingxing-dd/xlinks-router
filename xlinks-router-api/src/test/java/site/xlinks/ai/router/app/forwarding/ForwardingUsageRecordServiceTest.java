package site.xlinks.ai.router.app.forwarding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.app.forwarding.model.ForwardingUsageContext;
import site.xlinks.ai.router.app.forwarding.model.UsageMetrics;
import site.xlinks.ai.router.infrastructure.cache.RoutingSnapshotCacheService;
import site.xlinks.ai.router.mapper.UsageRecordMapper;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForwardingUsageRecordServiceTest {

    private UsageRecordMapper usageRecordMapper;
    private CustomerPlanService customerPlanService;
    private CustomerTokenQuotaService customerTokenQuotaService;
    private ProviderTokenQuotaService providerTokenQuotaService;
    private WalletService walletService;
    private RoutingSnapshotCacheService routingSnapshotCacheService;
    private ForwardingUsageRecordService forwardingUsageRecordService;

    @BeforeEach
    void setUp() {
        usageRecordMapper = mock(UsageRecordMapper.class);
        customerPlanService = mock(CustomerPlanService.class);
        customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        providerTokenQuotaService = mock(ProviderTokenQuotaService.class);
        walletService = mock(WalletService.class);
        routingSnapshotCacheService = mock(RoutingSnapshotCacheService.class);
        forwardingUsageRecordService = new ForwardingUsageRecordService(
                usageRecordMapper,
                customerPlanService,
                customerTokenQuotaService,
                providerTokenQuotaService,
                walletService,
                routingSnapshotCacheService
        );
    }

    @Test
    void shouldRefreshWalletSnapshotAfterBalanceSettlement() {
        ForwardingUsageContext context = ForwardingUsageContext.builder()
                .requestId("req-1")
                .accountId(101L)
                .customerTokenValue("customer-token")
                .modelCode("gpt-5.4")
                .modelProvider("OPENAI")
                .inputPrice(new BigDecimal("1.000000"))
                .outputPrice(new BigDecimal("1.000000"))
                .multiplier(BigDecimal.ONE)
                .build();
        UsageMetrics usageMetrics = UsageMetrics.builder()
                .inputTokens(1000)
                .outputTokens(1000)
                .totalTokens(2000)
                .build();
        when(walletService.debitBasicAllowOverdraftToZero(anyLong(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new WalletService.BasicWalletDebitResult(null, BigDecimal.ZERO, new BigDecimal("0.002000")));

        forwardingUsageRecordService.record(context, usageMetrics, 100L, null, null, "success");

        verify(walletService).debitBasicAllowOverdraftToZero(eq(101L), eq(new BigDecimal("0.002000")), anyString(), eq("req-1"), anyString());
        verify(routingSnapshotCacheService).refreshWalletByAccountId(101L);
        verify(providerTokenQuotaService).syncUsage(null, 2000);
    }
}
