package site.xlinks.ai.router.distributed.support.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.distributed.support.handler.GlobalExceptionHandler;
import site.xlinks.ai.router.distributed.support.service.UserBalanceQueryService;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserBalanceControllerTest {

    private ForwardingReadModelLoader readModelLoader;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        readModelLoader = mock(ForwardingReadModelLoader.class);
        UserBalanceQueryService service = new UserBalanceQueryService(new CustomerTokenResolver(), readModelLoader);
        UserBalanceController controller = new UserBalanceController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void balanceShouldReturnAggregatedUsdAmount() throws Exception {
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(1L);
        customerToken.setAccountId(101L);
        customerToken.setStatus(1);
        customerToken.setExpireTime(LocalDateTime.now().plusHours(1));

        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(101L);
        customerAccount.setStatus(1);
        customerAccount.setDeleted(0);

        CustomerMainWallet wallet = new CustomerMainWallet();
        wallet.setStatus(1);
        wallet.setAllowOut(1);
        wallet.setAvailableBalance(new BigDecimal("12.34"));

        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadCustomerAccount(101L)).thenReturn(customerAccount);
        when(readModelLoader.loadCustomerMainWallet(101L)).thenReturn(wallet);
        when(readModelLoader.loadAvailablePlans(101L)).thenReturn(List.of());

        mockMvc.perform(get("/user/balance")
                        .header("Authorization", "Bearer customer-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.balance").value(12.34))
                .andExpect(jsonPath("$.unit").value("USD"));
    }

    @Test
    void balanceShouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/user/balance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(4602))
                .andExpect(jsonPath("$.message").value("Missing customer token"));
    }
}
