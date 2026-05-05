package site.xlinks.ai.router.distributed.infrastructure.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.distributed.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderFailureState;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProviderRuntimeStateService {

    private final DistributedRouteCacheRepository distributedRouteCacheRepository;

    public void recordFailure(RoutingDecision routingDecision) {
        if (routingDecision == null) {
            return;
        }
        if (routingDecision.getProvider() != null && routingDecision.getProvider().getId() != null) {
            incrementProviderFailure(routingDecision.getProvider().getId());
        }
        if (routingDecision.getProviderToken() != null && routingDecision.getProviderToken().getId() != null) {
            incrementProviderTokenFailure(routingDecision.getProviderToken().getId());
        }
    }

    public void clearFailure(RoutingDecision routingDecision) {
        if (routingDecision == null) {
            return;
        }
        if (routingDecision.getProvider() != null && routingDecision.getProvider().getId() != null) {
            distributedRouteCacheRepository.deleteProviderFailureState(routingDecision.getProvider().getId());
        }
        if (routingDecision.getProviderToken() != null && routingDecision.getProviderToken().getId() != null) {
            distributedRouteCacheRepository.deleteProviderTokenFailureState(routingDecision.getProviderToken().getId());
        }
    }

    private void incrementProviderFailure(Long providerId) {
        ProviderFailureState current = distributedRouteCacheRepository.getProviderFailureState(providerId);
        distributedRouteCacheRepository.putProviderFailureState(
                providerId,
                new ProviderFailureState(current == null ? 1 : current.getFailureCount() + 1,
                        current == null ? Instant.now() : current.getFirstFailureAt())
        );
    }

    private void incrementProviderTokenFailure(Long providerTokenId) {
        ProviderFailureState current = distributedRouteCacheRepository.getProviderTokenFailureState(providerTokenId);
        distributedRouteCacheRepository.putProviderTokenFailureState(
                providerTokenId,
                new ProviderFailureState(current == null ? 1 : current.getFailureCount() + 1,
                        current == null ? Instant.now() : current.getFirstFailureAt())
        );
    }
}
