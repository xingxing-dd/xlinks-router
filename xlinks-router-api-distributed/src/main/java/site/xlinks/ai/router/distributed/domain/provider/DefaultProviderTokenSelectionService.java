package site.xlinks.ai.router.distributed.domain.provider;

import org.springframework.stereotype.Service;
import site.xlinks.ai.router.distributed.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class DefaultProviderTokenSelectionService implements ProviderTokenSelectionService {

    private final DistributedRouteCacheRepository distributedRouteCacheRepository;

    @Override
    public ProviderToken select(Provider provider, List<ProviderToken> candidates) {
        if (provider == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return candidates.stream()
                .filter(token -> token != null)
                .filter(token -> !isTemporarilyUnavailable(token.getId()))
                .filter(token -> token.getTokenStatus() != null && token.getTokenStatus() == 1)
                .filter(token -> token.getExpireTime() == null || !now.isAfter(token.getExpireTime()))
                .filter(token -> hasQuota(token))
                .sorted(tokenComparator())
                .findFirst()
                .orElse(null);
    }

    private boolean isTemporarilyUnavailable(Long providerTokenId) {
        if (providerTokenId == null) {
            return false;
        }
        ProviderFailureState state = distributedRouteCacheRepository.getProviderTokenFailureState(providerTokenId);
        return state != null && state.getFailureCount() > 0;
    }

    private boolean hasQuota(ProviderToken token) {
        Long total = token.getQuotaTotal();
        Long used = token.getQuotaUsed();
        return total == null || used == null || used < total;
    }

    private Comparator<ProviderToken> tokenComparator() {
        return Comparator
                .comparingLong(this::remainingQuota)
                .reversed()
                .thenComparing(ProviderToken::getLastUsedAt, this::compareLastUsed)
                .thenComparing(token -> token.getId() == null ? Long.MAX_VALUE : token.getId());
    }

    private long remainingQuota(ProviderToken token) {
        if (token == null) {
            return Long.MIN_VALUE;
        }
        Long total = token.getQuotaTotal();
        if (total == null) {
            return Long.MAX_VALUE;
        }
        long used = token.getQuotaUsed() == null ? 0L : token.getQuotaUsed();
        return total - used;
    }

    private int compareLastUsed(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }
}
