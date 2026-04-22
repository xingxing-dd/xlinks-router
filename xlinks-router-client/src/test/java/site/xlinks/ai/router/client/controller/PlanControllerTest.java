package site.xlinks.ai.router.client.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.plan.HistoricalSubscriptionResponse;
import site.xlinks.ai.router.client.service.ActivationCodeService;
import site.xlinks.ai.router.client.service.PlanOrderService;
import site.xlinks.ai.router.client.service.PlanService;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    @Mock
    private PlanService planService;

    @Mock
    private PlanOrderService planOrderService;

    @Mock
    private ActivationCodeService activationCodeService;

    @Mock
    private CustomerPlanMapper customerPlanMapper;

    @AfterEach
    void tearDown() {
        CustomerAccountContext.clear();
    }

    @Test
    void shouldReturnZeroUsedPercentageWhenHistoricalPlanTotalQuotaIsZero() {
        PlanController controller = new PlanController(
                planService,
                planOrderService,
                activationCodeService,
                customerPlanMapper
        );

        CustomerAccount account = new CustomerAccount();
        account.setId(1001L);
        CustomerAccountContext.setAccount(account);

        CustomerPlan plan = new CustomerPlan();
        plan.setId(2001L);
        plan.setPlanId(3001L);
        plan.setPlanName("Zero Quota Plan");
        plan.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        plan.setPlanExpireTime(LocalDateTime.of(2026, 4, 30, 10, 0));
        plan.setTotalQuota(BigDecimal.ZERO);
        plan.setTotalUsedQuota(new BigDecimal("12.345678"));
        plan.setStatus(1);

        when(customerPlanMapper.selectList(any())).thenReturn(List.of(plan));

        Result<List<HistoricalSubscriptionResponse>> result = controller.getHistoricalSubscriptions();

        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(0, result.getData().get(0).getUsedPercentage());
        assertEquals(new BigDecimal("12.345678"), result.getData().get(0).getUsedQuota());
    }
}
