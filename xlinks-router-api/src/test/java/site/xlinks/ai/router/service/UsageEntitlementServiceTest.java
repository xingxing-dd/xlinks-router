package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.model.wallet.WalletBundle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageEntitlementServiceTest {

    @Test
    void shouldUseBalanceWhenNoPlanAndWalletHasBalance() {
        CustomerPlanMapper customerPlanMapper = mock(CustomerPlanMapper.class);
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService service = new UsageEntitlementService(
                new ObjectMapper(),
                customerPlanMapper,
                routeCacheService,
                walletService
        );

        CustomerToken token = buildToken();
        when(customerPlanMapper.selectAvailablePlans(eq(100L), any())).thenReturn(List.of());
        when(walletService.ensureWallet(100L)).thenReturn(new WalletBundle(buildWallet("10.000000"), List.of()));

        UsageDecision decision = service.decide(token, "gpt-5");

        assertNull(decision.getPlanId());
        assertFalse(decision.isPackageEnabled());
        assertTrue(decision.isBalanceEnabled());
        assertEquals(2, decision.getCurrentUsageType());
    }

    @Test
    void shouldPreferPlanAndSkipBalanceCheckWhenPlanIsUsable() {
        CustomerPlanMapper customerPlanMapper = mock(CustomerPlanMapper.class);
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService service = new UsageEntitlementService(
                new ObjectMapper(),
                customerPlanMapper,
                routeCacheService,
                walletService
        );

        CustomerToken token = buildToken();
        CustomerPlan plan = buildPlan();
        when(customerPlanMapper.selectAvailablePlans(eq(100L), any())).thenReturn(List.of(plan));
        when(routeCacheService.isModelSupportedByPlan(200L, "gpt-5")).thenReturn(true);

        UsageDecision decision = service.decide(token, "gpt-5");

        assertEquals(300L, decision.getPlanId());
        assertTrue(decision.isPackageEnabled());
        assertFalse(decision.isBalanceEnabled());
        assertEquals(1, decision.getCurrentUsageType());
        verify(walletService, never()).ensureWallet(100L);
    }

    @Test
    void shouldRejectWhenNeitherPlanNorBalanceIsAvailable() {
        CustomerPlanMapper customerPlanMapper = mock(CustomerPlanMapper.class);
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        WalletService walletService = mock(WalletService.class);
        UsageEntitlementService service = new UsageEntitlementService(
                new ObjectMapper(),
                customerPlanMapper,
                routeCacheService,
                walletService
        );

        CustomerToken token = buildToken();
        when(customerPlanMapper.selectAvailablePlans(eq(100L), any())).thenReturn(List.of());
        when(walletService.ensureWallet(100L)).thenReturn(new WalletBundle(buildWallet("0.000000"), List.of()));

        UsageDecision decision = service.decide(token, "gpt-5");

        assertFalse(decision.isPackageEnabled());
        assertFalse(decision.isBalanceEnabled());
        assertEquals(0, decision.getCurrentUsageType());
    }

    private CustomerToken buildToken() {
        CustomerToken token = new CustomerToken();
        token.setId(10L);
        token.setAccountId(100L);
        token.setCustomerName("merchant");
        return token;
    }

    private CustomerPlan buildPlan() {
        CustomerPlan plan = new CustomerPlan();
        plan.setId(300L);
        plan.setPlanId(200L);
        plan.setStatus(1);
        plan.setDailyQuota(new BigDecimal("10.00"));
        plan.setUsedQuota(BigDecimal.ZERO);
        plan.setTotalQuota(new BigDecimal("100.00"));
        plan.setTotalUsedQuota(BigDecimal.ZERO);
        plan.setPlanExpireTime(LocalDateTime.now().plusDays(1));
        plan.setQuotaRefreshTime(LocalDateTime.now());
        return plan;
    }

    private CustomerMainWallet buildWallet(String availableBalance) {
        CustomerMainWallet wallet = new CustomerMainWallet();
        wallet.setStatus(1);
        wallet.setAllowOut(1);
        wallet.setAvailableBalance(new BigDecimal(availableBalance));
        return wallet;
    }
}
