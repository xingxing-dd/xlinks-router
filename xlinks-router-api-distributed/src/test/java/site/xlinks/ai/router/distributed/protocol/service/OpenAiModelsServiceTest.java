package site.xlinks.ai.router.distributed.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiModelsServiceTest {

    @Test
    void listModelsShouldFilterByTokenAndPlanRules() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        ProtocolRequestContextResolver protocolRequestContextResolver = mock(ProtocolRequestContextResolver.class);
        OpenAiModelsService service = new OpenAiModelsService(readModelLoader, protocolRequestContextResolver);
        HttpServletRequest request = mock(HttpServletRequest.class);

        CustomerTokenResolver.ResolvedCustomerToken resolvedCustomerToken =
                new CustomerTokenResolver.ResolvedCustomerToken("customer-token", null);
        CustomerToken customerToken = activeCustomerToken();
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(101L);
        customerAccount.setStatus(1);
        customerAccount.setDeleted(0);

        CustomerPlan supportedPlan = new CustomerPlan();
        supportedPlan.setPlanId(501L);
        supportedPlan.setPlanName("Starter");

        Model firstModel = model(201L, "gpt-4o-mini", LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        Model secondModel = model(202L, "gpt-4o", LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        Model thirdModel = model(203L, "claude-sonnet", LocalDateTime.of(2026, 1, 2, 3, 4, 5));

        when(protocolRequestContextResolver.resolveCustomerToken(request)).thenReturn(resolvedCustomerToken);
        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadCustomerAccount(101L)).thenReturn(customerAccount);
        when(readModelLoader.loadCustomerAllowedModelsRule(customerToken))
                .thenReturn(ruleWithModels("gpt-4o-mini", "gpt-4o"));
        when(readModelLoader.loadAvailablePlans(101L)).thenReturn(List.of(supportedPlan));
        when(readModelLoader.loadCustomerMainWallet(101L)).thenReturn(null);
        when(readModelLoader.loadPlanAllowedModelsRule(supportedPlan)).thenReturn(ruleWithModels("gpt-4o-mini"));
        when(readModelLoader.loadEnabledModels()).thenReturn(List.of(firstModel, secondModel, thirdModel));

        Map<String, Object> response = service.listModels(request);

        assertEquals("list", response.get("object"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        assertEquals(1, data.size());
        assertEquals("gpt-4o-mini", data.get(0).get("id"));
        assertEquals("model", data.get(0).get("object"));
        assertEquals("xlinks-router", data.get(0).get("owned_by"));
        assertEquals(1767323045L, data.get(0).get("created"));
    }

    @Test
    void listModelsShouldIncludeAllowedModelsWhenBalanceIsUsable() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        ProtocolRequestContextResolver protocolRequestContextResolver = mock(ProtocolRequestContextResolver.class);
        OpenAiModelsService service = new OpenAiModelsService(readModelLoader, protocolRequestContextResolver);
        HttpServletRequest request = mock(HttpServletRequest.class);

        CustomerTokenResolver.ResolvedCustomerToken resolvedCustomerToken =
                new CustomerTokenResolver.ResolvedCustomerToken("customer-token", null);
        CustomerToken customerToken = activeCustomerToken();
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(101L);
        customerAccount.setStatus(1);
        customerAccount.setDeleted(0);
        CustomerMainWallet wallet = new CustomerMainWallet();
        wallet.setStatus(1);
        wallet.setAllowOut(1);
        wallet.setAvailableBalance(new BigDecimal("8.88"));

        when(protocolRequestContextResolver.resolveCustomerToken(request)).thenReturn(resolvedCustomerToken);
        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadCustomerAccount(101L)).thenReturn(customerAccount);
        when(readModelLoader.loadCustomerAllowedModelsRule(customerToken))
                .thenReturn(ruleWithModels("gpt-4o-mini", "gpt-4o"));
        when(readModelLoader.loadAvailablePlans(101L)).thenReturn(List.of());
        when(readModelLoader.loadCustomerMainWallet(101L)).thenReturn(wallet);
        when(readModelLoader.loadEnabledModels()).thenReturn(List.of(
                model(201L, "gpt-4o-mini", LocalDateTime.now()),
                model(202L, "gpt-4o", LocalDateTime.now()),
                model(203L, "claude-sonnet", LocalDateTime.now())
        ));

        Map<String, Object> response = service.listModels(request);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        assertEquals(2, data.size());
        assertEquals(List.of("gpt-4o-mini", "gpt-4o"), data.stream().map(item -> String.valueOf(item.get("id"))).toList());
    }

    @Test
    void listModelsShouldRejectExpiredToken() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        ProtocolRequestContextResolver protocolRequestContextResolver = mock(ProtocolRequestContextResolver.class);
        OpenAiModelsService service = new OpenAiModelsService(readModelLoader, protocolRequestContextResolver);
        HttpServletRequest request = mock(HttpServletRequest.class);

        CustomerTokenResolver.ResolvedCustomerToken resolvedCustomerToken =
                new CustomerTokenResolver.ResolvedCustomerToken("customer-token", null);
        CustomerToken customerToken = activeCustomerToken();
        customerToken.setExpireTime(LocalDateTime.now().minusMinutes(1));

        when(protocolRequestContextResolver.resolveCustomerToken(request)).thenReturn(resolvedCustomerToken);
        when(readModelLoader.loadCustomerToken("customer-token")).thenReturn(customerToken);
        when(readModelLoader.loadFreshCustomerToken(customerToken, "customer-token")).thenReturn(customerToken);

        assertThrows(BusinessException.class, () -> service.listModels(request));
    }

    private CustomerToken activeCustomerToken() {
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(1L);
        customerToken.setAccountId(101L);
        customerToken.setStatus(1);
        customerToken.setExpireTime(LocalDateTime.now().plusHours(1));
        return customerToken;
    }

    private AllowedModelsRule ruleWithModels(String... modelCodes) {
        List<String> list = List.of(modelCodes);
        return new AllowedModelsRule(false, Set.copyOf(list), list);
    }

    private Model model(Long id, String modelCode, LocalDateTime createdAt) {
        Model model = new Model();
        model.setId(id);
        model.setModelCode(modelCode);
        model.setCreatedAt(createdAt);
        return model;
    }
}
