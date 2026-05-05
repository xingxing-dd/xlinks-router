package site.xlinks.ai.router.domain.provider;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderFailureState;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultProviderTokenSelectionServiceTest {

    @Test
    void shouldDistributeSelectionsAcrossProviderTokensInRoundRobinOrder() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        AtomicLong cursor = new AtomicLong(0L);
        when(cacheRepository.nextProviderTokenCursor(1001L)).thenAnswer(invocation -> cursor.incrementAndGet());
        DefaultProviderTokenSelectionService selectionService = new DefaultProviderTokenSelectionService(cacheRepository);

        Provider provider = provider(1001L);
        ProviderToken token1 = token(2001L);
        ProviderToken token2 = token(2002L);
        ProviderToken token3 = token(2003L);

        List<ProviderToken> candidates = List.of(token1, token2, token3);

        assertEquals(2001L, selectionService.select(provider, candidates).getId());
        assertEquals(2002L, selectionService.select(provider, candidates).getId());
        assertEquals(2003L, selectionService.select(provider, candidates).getId());
        assertEquals(2001L, selectionService.select(provider, candidates).getId());
    }

    @Test
    void shouldSkipUnavailableTokensBeforeApplyingRoundRobin() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        when(cacheRepository.nextProviderTokenCursor(1001L)).thenReturn(2L);
        when(cacheRepository.getProviderTokenFailureState(2002L))
                .thenReturn(new ProviderFailureState(1, Instant.now()));
        DefaultProviderTokenSelectionService selectionService = new DefaultProviderTokenSelectionService(cacheRepository);

        Provider provider = provider(1001L);
        ProviderToken token1 = token(2001L);
        ProviderToken token2 = token(2002L);
        ProviderToken token3 = token(2003L);

        ProviderToken selected = selectionService.select(provider, List.of(token1, token2, token3));

        assertEquals(2003L, selected.getId());
    }

    @Test
    void shouldReturnNullWhenNoEligibleProviderTokenExists() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        DefaultProviderTokenSelectionService selectionService = new DefaultProviderTokenSelectionService(cacheRepository);

        Provider provider = provider(1001L);
        ProviderToken disabled = token(2001L);
        disabled.setTokenStatus(0);

        assertNull(selectionService.select(provider, List.of(disabled)));
    }

    private Provider provider(Long id) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setProviderCode("provider-" + id);
        provider.setProviderName("provider-" + id);
        return provider;
    }

    private ProviderToken token(Long id) {
        ProviderToken token = new ProviderToken();
        token.setId(id);
        token.setProviderId(1001L);
        token.setTokenName("token-" + id);
        token.setTokenStatus(1);
        token.setExpireTime(LocalDateTime.now().plusHours(1));
        token.setQuotaTotal(100L);
        token.setQuotaUsed(0L);
        return token;
    }
}
