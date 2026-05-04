package site.xlinks.ai.router.distributed.domain.routing;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.domain.provider.ProviderTokenSelectionService;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingContext;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.distributed.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultRoutingDomainServiceTest {

    @Test
    void shouldPreferMerchantConfiguredProvider() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        ProviderTokenSelectionService providerTokenSelectionService = mock(ProviderTokenSelectionService.class);
        DefaultRoutingDomainService routingDomainService = new DefaultRoutingDomainService(
                cacheRepository,
                readModelLoader,
                providerTokenSelectionService
        );

        RoutingContext context = buildContext("gpt-4o-mini");
        ProviderModel first = providerModel(101L, 1001L);
        ProviderModel preferred = providerModel(102L, 1002L);
        Provider preferredProvider = provider(1002L, 10);
        ProviderToken preferredToken = providerToken(2002L, 1002L);

        when(readModelLoader.loadCustomerAllowedModelsRule(context.getCustomerToken()))
                .thenReturn(new AllowedModelsRule(true, Collections.emptySet(), Collections.emptyList()));
        when(readModelLoader.loadPlanAllowedModelsRule(context.getCustomerPlan()))
                .thenReturn(new AllowedModelsRule(true, Collections.emptySet(), Collections.emptyList()));
        when(readModelLoader.loadRoutingIndex(301L, ForwardProtocol.CHAT_COMPLETIONS))
                .thenReturn(List.of(first, preferred));
        when(readModelLoader.loadMerchantPreferredProvider(201L, 301L)).thenReturn(1002L);
        when(cacheRepository.getProviderFailureState(1002L)).thenReturn(null);
        when(readModelLoader.loadProvider(1002L)).thenReturn(preferredProvider);
        when(readModelLoader.loadProviderTokens(1002L)).thenReturn(List.of(preferredToken));
        when(providerTokenSelectionService.select(preferredProvider, List.of(preferredToken))).thenReturn(preferredToken);

        RoutingDecision decision = routingDomainService.route(context);

        assertEquals(1002L, decision.getProvider().getId());
        assertEquals(2002L, decision.getProviderToken().getId());
        assertEquals("ROUTED", decision.getDecisionStage());
    }

    @Test
    void shouldRejectModelNotAllowedByCustomerToken() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        ProviderTokenSelectionService providerTokenSelectionService = mock(ProviderTokenSelectionService.class);
        DefaultRoutingDomainService routingDomainService = new DefaultRoutingDomainService(
                cacheRepository,
                readModelLoader,
                providerTokenSelectionService
        );

        RoutingContext context = buildContext("gpt-4o-mini");
        when(readModelLoader.loadCustomerAllowedModelsRule(context.getCustomerToken()))
                .thenReturn(new AllowedModelsRule(false, Collections.singleton("gpt-4.1"), List.of("gpt-4.1")));

        BusinessException ex = assertThrows(BusinessException.class, () -> routingDomainService.route(context));
        assertEquals(ErrorCode.MODEL_NOT_IN_ALLOWED_LIST.getCode(), ex.getCode());
    }

    @Test
    void shouldSkipTemporarilyUnavailableProvider() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        ForwardingReadModelLoader readModelLoader = mock(ForwardingReadModelLoader.class);
        ProviderTokenSelectionService providerTokenSelectionService = mock(ProviderTokenSelectionService.class);
        DefaultRoutingDomainService routingDomainService = new DefaultRoutingDomainService(
                cacheRepository,
                readModelLoader,
                providerTokenSelectionService
        );

        RoutingContext context = buildContext("gpt-4o-mini");
        ProviderModel unavailable = providerModel(101L, 1001L);
        ProviderModel available = providerModel(102L, 1002L);
        Provider provider = provider(1002L, 1);
        ProviderToken token = providerToken(2002L, 1002L);

        when(readModelLoader.loadCustomerAllowedModelsRule(context.getCustomerToken()))
                .thenReturn(new AllowedModelsRule(true, Collections.emptySet(), Collections.emptyList()));
        when(readModelLoader.loadPlanAllowedModelsRule(context.getCustomerPlan()))
                .thenReturn(new AllowedModelsRule(true, Collections.emptySet(), Collections.emptyList()));
        when(readModelLoader.loadRoutingIndex(301L, ForwardProtocol.CHAT_COMPLETIONS))
                .thenReturn(List.of(unavailable, available));
        when(readModelLoader.loadMerchantPreferredProvider(201L, 301L)).thenReturn(null);
        when(cacheRepository.getProviderFailureState(1001L))
                .thenReturn(new ProviderFailureState(2, Instant.now()));
        when(cacheRepository.getProviderFailureState(1002L)).thenReturn(null);
        when(readModelLoader.loadProvider(1002L)).thenReturn(provider);
        when(readModelLoader.loadProviderTokens(1002L)).thenReturn(List.of(token));
        when(providerTokenSelectionService.select(provider, List.of(token))).thenReturn(token);

        RoutingDecision decision = routingDomainService.route(context);

        assertEquals(1002L, decision.getProvider().getId());
        assertEquals(2002L, decision.getProviderToken().getId());
    }

    private RoutingContext buildContext(String modelCode) {
        CustomerToken customerToken = new CustomerToken();
        customerToken.setId(101L);
        customerToken.setAccountId(201L);

        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(201L);

        CustomerPlan customerPlan = new CustomerPlan();
        customerPlan.setId(401L);
        customerPlan.setPlanId(501L);

        Model model = new Model();
        model.setId(301L);
        model.setModelCode(modelCode);

        ForwardRequest request = ForwardRequest.builder()
                .protocol(ForwardProtocol.CHAT_COMPLETIONS)
                .model(modelCode)
                .customerToken("customer-token")
                .requestBody("{\"model\":\"" + modelCode + "\"}")
                .stream(false)
                .build();

        return RoutingContext.builder()
                .request(request)
                .customerToken(customerToken)
                .customerAccount(customerAccount)
                .customerPlan(customerPlan)
                .model(model)
                .build();
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

    private Provider provider(Long id, Integer priority) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setStatus(1);
        provider.setPriority(priority);
        return provider;
    }

    private ProviderToken providerToken(Long id, Long providerId) {
        ProviderToken providerToken = new ProviderToken();
        providerToken.setId(id);
        providerToken.setProviderId(providerId);
        providerToken.setTokenStatus(1);
        return providerToken;
    }
}
