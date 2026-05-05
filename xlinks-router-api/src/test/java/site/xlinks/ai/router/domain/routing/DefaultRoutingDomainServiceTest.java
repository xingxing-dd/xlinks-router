package site.xlinks.ai.router.domain.routing;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.app.forwarding.model.ForwardingConsumptionMode;
import site.xlinks.ai.router.domain.routing.filter.AllowedModelsRoutingFilter;
import site.xlinks.ai.router.domain.routing.filter.ConsumptionDecisionRoutingFilter;
import site.xlinks.ai.router.domain.routing.filter.CustomerTokenValidationRoutingFilter;
import site.xlinks.ai.router.domain.routing.filter.ProviderCandidateRoutingFilter;
import site.xlinks.ai.router.domain.routing.filter.RoutingContextValidationFilter;
import site.xlinks.ai.router.domain.routing.filter.RoutingReadModelLoadingFilter;
import site.xlinks.ai.router.domain.routing.model.RoutingDecisionStage;
import site.xlinks.ai.router.domain.routing.model.RoutingExclusions;
import site.xlinks.ai.router.domain.routing.model.RoutingPlan;
import site.xlinks.ai.router.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ProviderModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultRoutingDomainServiceTest {

    @Test
    void shouldPreferMerchantConfiguredProviderAndSelectPlanSupportingModel() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        DefaultRoutingDomainService routingDomainService = routingDomainService(readModelLoader);

        ForwardRequest request = buildRequest("gpt-4o-mini");
        RoutingFixture fixture = stubRoutingBase(readModelLoader, request, "gpt-4o-mini");
        ProviderModel first = providerModel(101L, 1001L);
        ProviderModel preferred = providerModel(102L, 1002L);

        CustomerPlan earlierUnsupportedPlan = plan(401L, 501L, "较早到期但不支持模型");
        CustomerPlan matchedPlan = plan(402L, 502L, "支持模型的套餐");

        when(readModelLoader.loadAvailablePlans(201L)).thenReturn(List.of(earlierUnsupportedPlan, matchedPlan));
        when(readModelLoader.loadCustomerAllowedModelsRule(any())).thenReturn(allowAllRule());
        when(readModelLoader.loadPlanAllowedModelsRule(earlierUnsupportedPlan))
                .thenReturn(ruleWithModels("gpt-4.1"));
        when(readModelLoader.loadPlanAllowedModelsRule(matchedPlan))
                .thenReturn(ruleWithModels("gpt-4o-mini"));
        when(readModelLoader.loadRoutingIndex(301L, ForwardProtocol.CHAT_COMPLETIONS))
                .thenReturn(List.of(first, preferred));
        when(readModelLoader.loadMerchantPreferredProvider(201L, 301L)).thenReturn(1002L);

        RoutingPlan plan = routingDomainService.route(request);

        assertEquals(2, plan.getOrderedProviderModels().size());
        assertEquals(1002L, plan.getPreferredProviderId());
        assertEquals(1002L, plan.getOrderedProviderModels().get(0).getProviderId());
        assertEquals(ForwardingConsumptionMode.PLAN, plan.getConsumptionMode());
        assertEquals(matchedPlan.getId(), plan.getCustomerPlan().getId());
        assertEquals(fixture.model().getId(), plan.getModel().getId());
        assertEquals(RoutingDecisionStage.ROUTED, plan.getDecisionStage());
    }

    @Test
    void shouldRejectWhenCustomerTokenDailyQuotaReached() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        DefaultRoutingDomainService routingDomainService = routingDomainService(readModelLoader);

        ForwardRequest request = buildRequest("gpt-4o-mini");
        RoutingFixture fixture = stubRoutingBase(readModelLoader, request, "gpt-4o-mini");
        fixture.freshToken().setDailyQuota(BigDecimal.TEN);
        fixture.freshToken().setUsedQuota(BigDecimal.TEN);

        BusinessException ex = assertThrows(BusinessException.class, () -> routingDomainService.route(request));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void shouldFallbackToBalanceWhenNoPlanSupportsModel() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        DefaultRoutingDomainService routingDomainService = routingDomainService(readModelLoader);

        ForwardRequest request = buildRequest("gpt-4o-mini");
        stubRoutingBase(readModelLoader, request, "gpt-4o-mini");
        ProviderModel fallbackProvider = providerModel(102L, 1002L);

        CustomerPlan unsupportedPlan = plan(401L, 501L, "不支持当前模型的套餐");
        when(readModelLoader.loadAvailablePlans(201L)).thenReturn(List.of(unsupportedPlan));
        when(readModelLoader.loadPlanAllowedModelsRule(unsupportedPlan))
                .thenReturn(ruleWithModels("gpt-4.1"));
        when(readModelLoader.loadCustomerAllowedModelsRule(any())).thenReturn(allowAllRule());
        when(readModelLoader.loadRoutingIndex(301L, ForwardProtocol.CHAT_COMPLETIONS))
                .thenReturn(List.of(fallbackProvider));
        when(readModelLoader.loadMerchantPreferredProvider(201L, 301L)).thenReturn(null);
        when(readModelLoader.loadCustomerMainWallet(201L)).thenReturn(usableWallet(201L, new BigDecimal("12.34")));

        RoutingPlan plan = routingDomainService.route(request);

        assertEquals(ForwardingConsumptionMode.BALANCE, plan.getConsumptionMode());
        assertNull(plan.getCustomerPlan());
        assertEquals(1, plan.getOrderedProviderModels().size());
    }

    @Test
    void shouldRejectWhenWalletBalanceIsZero() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        DefaultRoutingDomainService routingDomainService = routingDomainService(readModelLoader);

        ForwardRequest request = buildRequest("gpt-4o-mini");
        stubRoutingBase(readModelLoader, request, "gpt-4o-mini");
        when(readModelLoader.loadAvailablePlans(201L)).thenReturn(List.of());
        when(readModelLoader.loadCustomerMainWallet(201L)).thenReturn(usableWallet(201L, BigDecimal.ZERO));

        BusinessException ex = assertThrows(BusinessException.class, () -> routingDomainService.route(request));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectModelNotAllowedByCustomerToken() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        DefaultRoutingDomainService routingDomainService = routingDomainService(readModelLoader);

        ForwardRequest request = buildRequest("gpt-4o-mini");
        stubRoutingBase(readModelLoader, request, "gpt-4o-mini");
        when(readModelLoader.loadAvailablePlans(201L)).thenReturn(List.of());
        when(readModelLoader.loadCustomerMainWallet(201L)).thenReturn(usableWallet(201L, new BigDecimal("9.99")));
        when(readModelLoader.loadCustomerAllowedModelsRule(any()))
                .thenReturn(ruleWithModels("gpt-4.1"));

        BusinessException ex = assertThrows(BusinessException.class, () -> routingDomainService.route(request));
        assertEquals(ErrorCode.MODEL_NOT_IN_ALLOWED_LIST.getCode(), ex.getCode());
    }

    @Test
    void shouldSkipExcludedProviderOnRoutingPlanBuild() {
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        DefaultRoutingDomainService routingDomainService = routingDomainService(readModelLoader);

        RoutingExclusions exclusions = RoutingExclusions.create();
        exclusions.excludeProviderId(1001L);
        ForwardRequest request = buildRequest("gpt-4o-mini");
        stubRoutingBase(readModelLoader, request, "gpt-4o-mini");
        ProviderModel first = providerModel(101L, 1001L);
        ProviderModel fallback = providerModel(102L, 1002L);

        CustomerPlan matchedPlan = plan(402L, 502L, "支持模型的套餐");
        when(readModelLoader.loadAvailablePlans(201L)).thenReturn(List.of(matchedPlan));
        when(readModelLoader.loadCustomerAllowedModelsRule(any())).thenReturn(allowAllRule());
        when(readModelLoader.loadPlanAllowedModelsRule(matchedPlan)).thenReturn(ruleWithModels("gpt-4o-mini"));
        when(readModelLoader.loadRoutingIndex(301L, ForwardProtocol.CHAT_COMPLETIONS))
                .thenReturn(List.of(first, fallback));
        when(readModelLoader.loadMerchantPreferredProvider(201L, 301L)).thenReturn(1001L);

        RoutingPlan plan = routingDomainService.route(request, exclusions);

        assertEquals(1, plan.getOrderedProviderModels().size());
        assertEquals(1002L, plan.getOrderedProviderModels().get(0).getProviderId());
    }

    private DefaultRoutingDomainService routingDomainService(ForwardingReadModelLoader readModelLoader) {
        return new DefaultRoutingDomainService(List.of(
                new RoutingContextValidationFilter(),
                new RoutingReadModelLoadingFilter(readModelLoader),
                new CustomerTokenValidationRoutingFilter(),
                new ConsumptionDecisionRoutingFilter(readModelLoader),
                new AllowedModelsRoutingFilter(readModelLoader),
                new ProviderCandidateRoutingFilter(readModelLoader)
        ));
    }

    private ForwardRequest buildRequest(String modelCode) {
        return ForwardRequest.builder()
                .protocol(ForwardProtocol.CHAT_COMPLETIONS)
                .model(modelCode)
                .customerToken("customer-token")
                .requestBody("{\"model\":\"" + modelCode + "\"}")
                .stream(false)
                .build();
    }

    private RoutingFixture stubRoutingBase(ForwardingReadModelLoader readModelLoader,
                                           ForwardRequest request,
                                           String modelCode) {
        CustomerToken cachedToken = token(101L, 201L);
        CustomerToken freshToken = token(101L, 201L);
        CustomerAccount customerAccount = account(201L);
        Model model = model(301L, modelCode);

        when(readModelLoader.loadCustomerToken(request.getCustomerToken())).thenReturn(cachedToken);
        when(readModelLoader.loadFreshCustomerToken(any(CustomerToken.class), eq(request.getCustomerToken()))).thenReturn(freshToken);
        when(readModelLoader.loadCustomerAccount(201L)).thenReturn(customerAccount);
        when(readModelLoader.loadModel(modelCode)).thenReturn(model);
        return new RoutingFixture(cachedToken, freshToken, customerAccount, model);
    }

    private CustomerToken token(Long tokenId, Long accountId) {
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(tokenId);
        customerToken.setAccountId(accountId);
        customerToken.setStatus(1);
        customerToken.setTokenName("测试令牌");
        customerToken.setCustomerName("测试客户");
        customerToken.setExpireTime(LocalDateTime.now().plusHours(1));
        customerToken.setDailyQuota(new BigDecimal("100"));
        customerToken.setUsedQuota(BigDecimal.ZERO);
        customerToken.setTotalQuota(new BigDecimal("1000"));
        customerToken.setTotalUsedQuota(BigDecimal.ZERO);
        return customerToken;
    }

    private CustomerAccount account(Long accountId) {
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(accountId);
        customerAccount.setUsername("tester@example.com");
        customerAccount.setStatus(1);
        return customerAccount;
    }

    private CustomerPlan plan(Long recordId, Long planId, String planName) {
        CustomerPlan customerPlan = new CustomerPlan();
        customerPlan.setId(recordId);
        customerPlan.setPlanId(planId);
        customerPlan.setPlanName(planName);
        customerPlan.setStatus(1);
        customerPlan.setPlanExpireTime(LocalDateTime.now().plusDays(1));
        customerPlan.setDailyQuota(new BigDecimal("100"));
        customerPlan.setUsedQuota(BigDecimal.ZERO);
        customerPlan.setTotalQuota(new BigDecimal("1000"));
        customerPlan.setTotalUsedQuota(BigDecimal.ZERO);
        customerPlan.setMultiplier(BigDecimal.ONE);
        return customerPlan;
    }

    private Model model(Long modelId, String modelCode) {
        Model model = new Model();
        model.setId(modelId);
        model.setModelCode(modelCode);
        model.setModelName("测试模型");
        return model;
    }

    private ProviderModel providerModel(Long id, Long providerId) {
        ProviderModel providerModel = new ProviderModel();
        providerModel.setId(id);
        providerModel.setProviderId(providerId);
        providerModel.setModelId(301L);
        providerModel.setStatus(1);
        providerModel.setDeleted(0);
        return providerModel;
    }

    private CustomerMainWallet usableWallet(Long accountId, BigDecimal availableBalance) {
        CustomerMainWallet wallet = new CustomerMainWallet();
        wallet.setAccountId(accountId);
        wallet.setStatus(1);
        wallet.setAllowOut(1);
        wallet.setAvailableBalance(availableBalance);
        return wallet;
    }

    private AllowedModelsRule allowAllRule() {
        return new AllowedModelsRule(true, Collections.emptySet(), Collections.emptyList());
    }

    private AllowedModelsRule ruleWithModels(String... models) {
        java.util.Set<String> allowedModels = java.util.Arrays.stream(models).collect(java.util.stream.Collectors.toSet());
        return new AllowedModelsRule(false, allowedModels, List.of(models));
    }

    private record RoutingFixture(CustomerToken cachedToken,
                                  CustomerToken freshToken,
                                  CustomerAccount customerAccount,
                                  Model model) {
    }
}
