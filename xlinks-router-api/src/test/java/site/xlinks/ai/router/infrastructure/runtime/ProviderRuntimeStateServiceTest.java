package site.xlinks.ai.router.infrastructure.runtime;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ProviderRuntimeStateServiceTest {

    private static final ProviderFailurePolicy PROVIDER_FAILURE_POLICY =
            new ProviderFailurePolicy(5, 300000L, 1800000L);

    @Test
    void shouldOnlyRecordTokenFailureWhenProviderTokenExists() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        ProviderRuntimeStateService runtimeStateService =
                new ProviderRuntimeStateService(cacheRepository, PROVIDER_FAILURE_POLICY);

        runtimeStateService.recordFailure(RoutingDecision.builder()
                .provider(provider(1001L))
                .providerToken(providerToken(2001L))
                .build());

        verify(cacheRepository, never()).getProviderFailureState(1001L);
        verify(cacheRepository, never()).putProviderFailureState(eq(1001L), any(), any());
        verify(cacheRepository).getProviderTokenFailureState(2001L);
        verify(cacheRepository).putProviderTokenFailureState(eq(2001L), any(), eq(PROVIDER_FAILURE_POLICY.tokenFailureStateTtl()));
    }

    @Test
    void shouldBlacklistTokenForFiveMinutesAfterFiveConsecutiveFailures() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        ProviderRuntimeStateService runtimeStateService =
                new ProviderRuntimeStateService(cacheRepository, PROVIDER_FAILURE_POLICY);
        Instant firstFailureAt = Instant.now().minusSeconds(30);
        when(cacheRepository.getProviderTokenFailureState(2001L))
                .thenReturn(new ProviderFailureState(4, firstFailureAt, null));

        runtimeStateService.recordFailure(RoutingDecision.builder()
                .providerToken(providerToken(2001L))
                .build());

        ArgumentCaptor<ProviderFailureState> captor = ArgumentCaptor.forClass(ProviderFailureState.class);
        verify(cacheRepository).putProviderTokenFailureState(eq(2001L), captor.capture(), eq(PROVIDER_FAILURE_POLICY.tokenFailureStateTtl()));
        ProviderFailureState savedState = captor.getValue();
        assertEquals(5, savedState.getFailureCount());
        assertEquals(firstFailureAt, savedState.getFirstFailureAt());
        assertNotNull(savedState.getBlockedUntil());
    }

    @Test
    void shouldRestartFailureCountingAfterBlacklistExpires() {
        DistributedRouteCacheRepository cacheRepository = mock(DistributedRouteCacheRepository.class);
        ProviderRuntimeStateService runtimeStateService =
                new ProviderRuntimeStateService(cacheRepository, PROVIDER_FAILURE_POLICY);
        when(cacheRepository.getProviderTokenFailureState(2001L))
                .thenReturn(new ProviderFailureState(
                        5,
                        Instant.now().minusSeconds(600),
                        Instant.now().minusSeconds(1)
                ));

        runtimeStateService.recordFailure(RoutingDecision.builder()
                .providerToken(providerToken(2001L))
                .build());

        ArgumentCaptor<ProviderFailureState> captor = ArgumentCaptor.forClass(ProviderFailureState.class);
        verify(cacheRepository).putProviderTokenFailureState(eq(2001L), captor.capture(), eq(PROVIDER_FAILURE_POLICY.tokenFailureStateTtl()));
        ProviderFailureState savedState = captor.getValue();
        assertEquals(1, savedState.getFailureCount());
        assertNull(savedState.getBlockedUntil());
    }

    private Provider provider(Long id) {
        Provider provider = new Provider();
        provider.setId(id);
        return provider;
    }

    private ProviderToken providerToken(Long id) {
        ProviderToken providerToken = new ProviderToken();
        providerToken.setId(id);
        return providerToken;
    }
}
