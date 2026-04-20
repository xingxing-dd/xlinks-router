package site.xlinks.ai.router.service.routing;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.ProviderTokenSelectService;
import site.xlinks.ai.router.service.RouteCacheService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderRouteResolverTest {

    @Test
    void shouldSkipTemporarilyUnavailableProviderAndUseNextCandidate() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        ProviderTokenSelectService providerTokenSelectService = mock(ProviderTokenSelectService.class);
        ProviderRouteResolver resolver = new ProviderRouteResolver(routeCacheService, providerTokenSelectService);

        ProviderModel first = new ProviderModel();
        first.setId(1L);
        first.setModelId(10L);
        first.setProviderId(100L);

        ProviderModel second = new ProviderModel();
        second.setId(2L);
        second.setModelId(10L);
        second.setProviderId(200L);

        Provider secondProvider = new Provider();
        secondProvider.setId(200L);
        secondProvider.setStatus(1);

        ProviderToken secondToken = new ProviderToken();
        secondToken.setId(300L);
        secondToken.setProviderId(200L);
        secondToken.setTokenStatus(1);

        when(routeCacheService.listProviderModelsByPriority(10L, ProxyProtocol.CHAT_COMPLETIONS))
                .thenReturn(List.of(first, second));
        when(routeCacheService.isProviderTemporarilyUnavailable(100L)).thenReturn(true);
        when(routeCacheService.isProviderTemporarilyUnavailable(200L)).thenReturn(false);
        when(routeCacheService.getProvider(200L)).thenReturn(secondProvider);
        when(providerTokenSelectService.selectTokenOrNull(200L)).thenReturn(secondToken);

        ProviderRouteResolver.ResolvedProviderRoute route = resolver.resolve(
                10L,
                "gpt-4o",
                ProxyProtocol.CHAT_COMPLETIONS
        );

        assertNotNull(route);
        assertEquals(200L, route.provider().getId());
        assertEquals(2L, route.providerModel().getId());
        assertEquals(300L, route.providerToken().getId());
        verify(routeCacheService, never()).getProvider(100L);
        verify(providerTokenSelectService, never()).selectTokenOrNull(100L);
    }
}
