package site.xlinks.ai.router.distributed.support.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserBalanceQueryServiceTest {

    @Test
    void queryBalanceShouldReturnWalletAndPlanTotal() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        UserBalanceQueryService service = new UserBalanceQueryService(new CustomerTokenResolver(), readModelLoader);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer customer-token");

        CustomerToken customerToken = activeCustomerToken();
        CustomerAccount customerAccount = activeCustomerAccount();
        CustomerMainWallet wallet = new CustomerMainWallet();
        wallet.setStatus(1);
        wallet.setAllowOut(1);
        wallet.setAvailableBalance(new BigDecimal("12.34"));

        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadCustomerAccount(101L)).thenReturn(customerAccount);
        when(readModelLoader.loadCustomerMainWallet(101L)).thenReturn(wallet);
        when(readModelLoader.loadAvailablePlans(101L)).thenReturn(List.of(
                availablePlan(
                        new BigDecimal("100"),
                        new BigDecimal("40"),
                        new BigDecimal("30"),
                        new BigDecimal("10"),
                        LocalDateTime.now().minusMinutes(5)
                ),
                availablePlan(
                        new BigDecimal("50"),
                        new BigDecimal("20"),
                        new BigDecimal("25"),
                        new BigDecimal("25"),
                        LocalDateTime.now().minusDays(1)
                )
        ));

        Map<String, Object> response = service.queryBalance(request);

        assertEquals(Boolean.TRUE, response.get("is_active"));
        assertEquals("USD", response.get("unit"));
        assertEquals(new BigDecimal("57.34"), response.get("balance"));
    }

    @Test
    void queryBalanceShouldIgnoreExpiredPlans() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        UserBalanceQueryService service = new UserBalanceQueryService(new CustomerTokenResolver(), readModelLoader);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer customer-token");

        CustomerToken customerToken = activeCustomerToken();
        CustomerAccount customerAccount = activeCustomerAccount();
        CustomerMainWallet wallet = new CustomerMainWallet();
        wallet.setStatus(1);
        wallet.setAllowOut(1);
        wallet.setAvailableBalance(new BigDecimal("10.00"));

        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadCustomerAccount(101L)).thenReturn(customerAccount);
        when(readModelLoader.loadCustomerMainWallet(101L)).thenReturn(wallet);
        when(readModelLoader.loadAvailablePlans(101L)).thenReturn(List.of(
                availablePlan(
                        new BigDecimal("100"),
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        new BigDecimal("5"),
                        LocalDateTime.now().minusMinutes(10)
                ),
                expiredPlan(
                        new BigDecimal("100"),
                        new BigDecimal("10"),
                        new BigDecimal("50"),
                        new BigDecimal("0"),
                        LocalDateTime.now().minusMinutes(10)
                )
        ));

        Map<String, Object> response = service.queryBalance(request);

        assertEquals(new BigDecimal("35.00"), response.get("balance"));
    }

    @Test
    void queryBalanceShouldRejectExpiredToken() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        UserBalanceQueryService service = new UserBalanceQueryService(new CustomerTokenResolver(), readModelLoader);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer customer-token");

        CustomerToken customerToken = activeCustomerToken();
        customerToken.setExpireTime(LocalDateTime.now().minusMinutes(1));

        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);

        assertThrows(BusinessException.class, () -> service.queryBalance(request));
    }

    private CustomerToken activeCustomerToken() {
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(1L);
        customerToken.setAccountId(101L);
        customerToken.setStatus(1);
        customerToken.setExpireTime(LocalDateTime.now().plusHours(1));
        return customerToken;
    }

    private CustomerAccount activeCustomerAccount() {
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(101L);
        customerAccount.setStatus(1);
        customerAccount.setDeleted(0);
        return customerAccount;
    }

    private CustomerPlan availablePlan(BigDecimal totalQuota,
                                       BigDecimal totalUsedQuota,
                                       BigDecimal dailyQuota,
                                       BigDecimal usedQuota,
                                       LocalDateTime refreshTime) {
        CustomerPlan plan = new CustomerPlan();
        plan.setStatus(1);
        plan.setTotalQuota(totalQuota);
        plan.setTotalUsedQuota(totalUsedQuota);
        plan.setDailyQuota(dailyQuota);
        plan.setUsedQuota(usedQuota);
        plan.setQuotaRefreshTime(refreshTime);
        plan.setPlanExpireTime(LocalDateTime.now().plusDays(1));
        return plan;
    }

    private CustomerPlan expiredPlan(BigDecimal totalQuota,
                                     BigDecimal totalUsedQuota,
                                     BigDecimal dailyQuota,
                                     BigDecimal usedQuota,
                                     LocalDateTime refreshTime) {
        CustomerPlan plan = availablePlan(totalQuota, totalUsedQuota, dailyQuota, usedQuota, refreshTime);
        plan.setPlanExpireTime(LocalDateTime.now().minusMinutes(1));
        return plan;
    }
}
