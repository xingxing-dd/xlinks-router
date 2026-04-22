package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.UsageRecordMapper;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UsageRecordServiceTest {

    @Test
    void shouldCalculateCostUsingCacheHitFormula() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        CustomerPlanService customerPlanService = mock(CustomerPlanService.class);
        CustomerTokenQuotaService customerTokenQuotaService = mock(CustomerTokenQuotaService.class);
        WalletService walletService = mock(WalletService.class);
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService);

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

        service.record(context, usageMetrics, 1200L, null, null);

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
        verify(walletService).debitBasic(
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
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService);

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

        service.record(context, usageMetrics, 800L, null, null);

        ArgumentCaptor<UsageRecord> captor = ArgumentCaptor.forClass(UsageRecord.class);
        verify(usageRecordMapper).insert(captor.capture());
        UsageRecord record = captor.getValue();

        assertEquals(0, record.getCacheHitTokens());
        assertDecimalEquals("0.000200", record.getPromptCost());
        assertDecimalEquals("0.000000", record.getCacheHitCost());
        assertDecimalEquals("0.000160", record.getCompletionCost());
        assertDecimalEquals("0.000360", record.getTotalCost());
        verify(walletService).debitBasic(
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
        UsageRecordService service = new UsageRecordService(usageRecordMapper, customerPlanService, customerTokenQuotaService, walletService);

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

        service.record(context, usageMetrics, 1200L, null, null);

        verify(customerPlanService).consumeQuota(900L, new BigDecimal("0.000360"));
        verify(walletService, never()).debitBasic(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
                () -> "Expected " + expected + " but was " + actual);
    }
}
