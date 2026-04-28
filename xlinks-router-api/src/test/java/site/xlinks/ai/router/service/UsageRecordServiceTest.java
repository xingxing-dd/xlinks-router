package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.UsageRecordMapper;
import site.xlinks.ai.router.model.wallet.WalletBundle;
import site.xlinks.ai.router.service.routing.RoutingBuildContext;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageRecordServiceTest {

    @Test
    void shouldCalculateCostUsingCacheHitFormula() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        CustomerPlanService customerPlanService = mock(CustomerPlanService.class);
        CustomerTokenQuotaService customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService usageEntitlementService = mock(UsageEntitlementService.class);
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService, usageEntitlementService);

        ProviderInvokeContext context = ProviderInvokeContext.builder()
                .requestId("req_1")
                .providerId(1L)
                .providerCode("openai")
                .providerName("OpenAI")
                .endpointCode("responses")
                .modelId(10L)
                .modelCode("gpt-x")
                .modelName("GPT-X")
                .customerToken("sk-user")
                .providerToken("sk-provider")
                .accountId(100L)
                .inputPrice(new BigDecimal("2"))
                .cacheHitPrice(new BigDecimal("0.5"))
                .outputPrice(new BigDecimal("8"))
                .multiplier(new BigDecimal("1.5"))
                .modelProvider("OPENAI")
                .build();

        UsageMetrics usageMetrics = UsageMetrics.builder()
                .inputTokens(100)
                .cacheHitTokens(40)
                .outputTokens(20)
                .totalTokens(120)
                .build();

        service.record(context, usageMetrics, 1200L, null, null, null);

        ArgumentCaptor<UsageRecord> captor = ArgumentCaptor.forClass(UsageRecord.class);
        verify(usageRecordMapper).insert(captor.capture());
        UsageRecord record = captor.getValue();

        assertEquals(100, record.getPromptTokens());
        assertEquals(40, record.getCacheHitTokens());
        assertEquals(20, record.getCompletionTokens());
        assertEquals(120, record.getTotalTokens());
        assertDecimalEquals("0.000120", record.getPromptCost());
        assertDecimalEquals("0.000030", record.getCacheHitCost());
        assertDecimalEquals("0.000160", record.getCompletionCost());
        assertDecimalEquals("0.000310", record.getTotalCost());
        verify(walletService).debitBasicAllowOverdraftToZero(
                100L,
                new BigDecimal("0.000310"),
                WalletConstants.BIZ_TYPE_API_USAGE,
                "req_1",
                "API usage completed: gpt-x"
        );
    }

    @Test
    void shouldIgnoreCacheHitTokensWhenProviderStrategyIsNone() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        CustomerPlanService customerPlanService = mock(CustomerPlanService.class);
        CustomerTokenQuotaService customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService usageEntitlementService = mock(UsageEntitlementService.class);
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService, usageEntitlementService);

        ProviderInvokeContext context = ProviderInvokeContext.builder()
                .requestId("req_2")
                .providerId(1L)
                .providerCode("provider")
                .providerName("Provider")
                .endpointCode("chat/completions")
                .modelId(11L)
                .modelCode("gpt-y")
                .modelName("GPT-Y")
                .customerToken("sk-user")
                .providerToken("sk-provider")
                .accountId(101L)
                .inputPrice(new BigDecimal("2"))
                .cacheHitPrice(new BigDecimal("0.5"))
                .outputPrice(new BigDecimal("8"))
                .modelProvider("CUSTOM_PROVIDER")
                .build();

        UsageMetrics usageMetrics = UsageMetrics.builder()
                .inputTokens(100)
                .cacheHitTokens(40)
                .outputTokens(20)
                .totalTokens(120)
                .build();

        service.record(context, usageMetrics, 800L, null, null, null);

        ArgumentCaptor<UsageRecord> captor = ArgumentCaptor.forClass(UsageRecord.class);
        verify(usageRecordMapper).insert(captor.capture());
        UsageRecord record = captor.getValue();

        assertEquals(0, record.getCacheHitTokens());
        assertDecimalEquals("0.000200", record.getPromptCost());
        assertDecimalEquals("0.000000", record.getCacheHitCost());
        assertDecimalEquals("0.000160", record.getCompletionCost());
        assertDecimalEquals("0.000360", record.getTotalCost());
        verify(walletService).debitBasicAllowOverdraftToZero(
                101L,
                new BigDecimal("0.000360"),
                WalletConstants.BIZ_TYPE_API_USAGE,
                "req_2",
                "API usage completed: gpt-y"
        );
    }

    @Test
    void shouldConsumePlanQuotaWhenPlanIsSelected() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        CustomerPlanService customerPlanService = mock(CustomerPlanService.class);
        CustomerTokenQuotaService customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService usageEntitlementService = mock(UsageEntitlementService.class);
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService, usageEntitlementService);

        ProviderInvokeContext context = ProviderInvokeContext.builder()
                .requestId("req_plan")
                .providerId(1L)
                .providerCode("openai")
                .providerName("OpenAI")
                .endpointCode("responses")
                .modelId(10L)
                .modelCode("gpt-x")
                .modelName("GPT-X")
                .customerToken("sk-user")
                .providerToken("sk-provider")
                .accountId(100L)
                .planId(900L)
                .inputPrice(new BigDecimal("2"))
                .outputPrice(new BigDecimal("8"))
                .modelProvider("OPENAI")
                .build();

        UsageMetrics usageMetrics = UsageMetrics.builder()
                .inputTokens(100)
                .outputTokens(20)
                .totalTokens(120)
                .build();

        CustomerMainWallet mainWallet = new CustomerMainWallet();
        mainWallet.setAvailableBalance(BigDecimal.ZERO);
        when(walletService.debitBasicAllowOverdraftToZero(anyLong(), any(), any(), any(), any()))
                .thenReturn(new WalletService.BasicWalletDebitResult(
                        new WalletBundle(mainWallet, java.util.List.of()),
                        new BigDecimal("0.000100"),
                        new BigDecimal("0.000260")
                ));

        service.record(context, usageMetrics, 1200L, null, null, null);

        verify(customerPlanService).consumeQuota(900L, new BigDecimal("0.000360"));
        verify(walletService, never()).debitBasicAllowOverdraftToZero(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldAllowBasicWalletOverdraftWhenRecordingUsage() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        CustomerPlanService customerPlanService = mock(CustomerPlanService.class);
        CustomerTokenQuotaService customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService usageEntitlementService = mock(UsageEntitlementService.class);
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService, usageEntitlementService);

        ProviderInvokeContext context = ProviderInvokeContext.builder()
                .requestId("req_overdraft")
                .providerId(1L)
                .providerCode("openai")
                .providerName("OpenAI")
                .endpointCode("responses")
                .modelId(10L)
                .modelCode("gpt-x")
                .modelName("GPT-X")
                .customerToken("sk-user")
                .customerTokenId(501L)
                .providerToken("sk-provider")
                .accountId(100L)
                .inputPrice(new BigDecimal("2"))
                .outputPrice(new BigDecimal("8"))
                .modelProvider("OPENAI")
                .build();

        UsageMetrics usageMetrics = UsageMetrics.builder()
                .inputTokens(100)
                .outputTokens(20)
                .totalTokens(120)
                .build();

        service.record(context, usageMetrics, 1200L, null, null, null);

        verify(usageRecordMapper).insert(any(UsageRecord.class));
        verify(walletService).debitBasicAllowOverdraftToZero(
                100L,
                new BigDecimal("0.000360"),
                WalletConstants.BIZ_TYPE_API_USAGE,
                "req_overdraft",
                "API usage completed: gpt-x"
        );
        verify(usageRecordMapper).sumTotalCostByDateRange(anyLong(), any(), any(), any());
        verify(customerTokenQuotaService).syncQuotaUsage(501L, BigDecimal.ZERO, new BigDecimal("0.000360"));
        verify(usageEntitlementService).syncBalanceAvailability(100L, BigDecimal.ZERO);
    }

    @Test
    void shouldRecordRoutingErrorAfterTokenValidation() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        CustomerPlanService customerPlanService = mock(CustomerPlanService.class);
        CustomerTokenQuotaService customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService usageEntitlementService = mock(UsageEntitlementService.class);
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService, usageEntitlementService);

        RoutingBuildContext context = new RoutingBuildContext(
                "sk-user",
                ProxyRequest.builder()
                        .protocol(ProxyProtocol.RESPONSES)
                        .model("gpt-x")
                        .build(),
                "req_route_fail",
                java.util.Set.of(),
                java.util.Set.of()
        );
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(501L);
        customerToken.setAccountId(100L);
        customerToken.setCustomerName("merchant-a");
        customerToken.setTokenName("token-a");
        context.setCustomerToken(customerToken);
        context.setUsageDecision(UsageDecision.builder()
                .customerTokenId(501L)
                .customerName("merchant-a")
                .balanceEnabled(true)
                .build());
        Model model = new Model();
        model.setId(10L);
        model.setModelCode("gpt-x");
        model.setModelName("GPT-X");
        context.setModel(model);

        service.recordRoutingError(
                context,
                500,
                String.valueOf(ErrorCode.ROUTE_ERROR.getCode()),
                "No available provider token for model and protocol: gpt-x",
                321L,
                "provider_token_unavailable"
        );

        ArgumentCaptor<UsageRecord> captor = ArgumentCaptor.forClass(UsageRecord.class);
        verify(usageRecordMapper).insert(captor.capture());
        UsageRecord record = captor.getValue();

        assertEquals("req_route_fail", record.getRequestId());
        assertEquals(100L, record.getAccountId());
        assertEquals("sk-user", record.getCustomerToken());
        assertEquals("balance", record.getUsageType());
        assertEquals("responses", record.getEndpointCode());
        assertEquals("gpt-x", record.getModelCode());
        assertEquals(0, record.getTotalTokens());
        assertDecimalEquals("0", record.getTotalCost());
        assertEquals("provider_token_unavailable", record.getFinishReason());
        assertEquals(String.valueOf(ErrorCode.ROUTE_ERROR.getCode()), record.getErrorCode());
        assertEquals(321, record.getSessionMs());
        verify(walletService, never()).debitBasicAllowOverdraftToZero(anyLong(), any(), any(), any(), any());
        verify(customerPlanService, never()).consumeQuota(anyLong(), any());
        verify(customerTokenQuotaService, never()).syncQuotaUsage(anyLong(), any(), any());
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
                () -> "Expected " + expected + " but was " + actual);
    }
}
