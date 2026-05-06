package site.xlinks.ai.router.infrastructure.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderFailureState;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProviderRuntimeStateService {

    private final DistributedRouteCacheRepository distributedRouteCacheRepository;
    private final ProviderFailurePolicy providerFailurePolicy;

    public void recordFailure(RoutingDecision routingDecision) {
        if (routingDecision == null) {
            return;
        }
        if (routingDecision.getProviderToken() != null && routingDecision.getProviderToken().getId() != null) {
            incrementProviderTokenFailure(routingDecision.getProviderToken().getId());
            return;
        }
        if (routingDecision.getProvider() != null && routingDecision.getProvider().getId() != null) {
            incrementProviderFailure(routingDecision.getProvider().getId());
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
        Instant now = Instant.now();
        ProviderFailureState current = distributedRouteCacheRepository.getProviderFailureState(providerId);
        distributedRouteCacheRepository.putProviderFailureState(
                providerId,
                nextFailureState(current, now),
                providerFailurePolicy.tokenFailureStateTtl()
        );
    }

    private void incrementProviderTokenFailure(Long providerTokenId) {
        Instant now = Instant.now();
        ProviderFailureState current = distributedRouteCacheRepository.getProviderTokenFailureState(providerTokenId);
        distributedRouteCacheRepository.putProviderTokenFailureState(
                providerTokenId,
                nextFailureState(current, now),
                providerFailurePolicy.tokenFailureStateTtl()
        );
    }

    private ProviderFailureState nextFailureState(ProviderFailureState current, Instant now) {
        ProviderFailureState normalized = normalizeExpiredBlockedState(current, now);
        int nextFailureCount = normalized == null ? 1 : normalized.getFailureCount() + 1;
        Instant firstFailureAt = normalized == null ? now : normalized.getFirstFailureAt();
        Instant blockedUntil = nextFailureCount >= providerFailurePolicy.tokenConsecutiveFailureThreshold()
                ? now.plus(providerFailurePolicy.tokenBlockedDuration())
                : null;
        return new ProviderFailureState(nextFailureCount, firstFailureAt, blockedUntil);
    }

    private ProviderFailureState normalizeExpiredBlockedState(ProviderFailureState current, Instant now) {
        if (current == null) {
            return null;
        }
        if (current.getBlockedUntil() != null && !current.getBlockedUntil().isAfter(now)) {
            return null;
        }
        return current;
    }
}
