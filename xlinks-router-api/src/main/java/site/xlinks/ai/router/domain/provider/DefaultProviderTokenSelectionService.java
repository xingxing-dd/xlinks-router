package site.xlinks.ai.router.domain.provider;

import org.springframework.stereotype.Service;
import site.xlinks.ai.router.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
        List<ProviderToken> eligibleTokens = candidates.stream()
                .filter(Objects::nonNull)
                .filter(token -> !isTemporarilyUnavailable(token.getId()))
                .filter(token -> token.getTokenStatus() != null && token.getTokenStatus() == 1)
                .filter(token -> token.getExpireTime() == null || !now.isAfter(token.getExpireTime()))
                .filter(this::hasQuota)
                .sorted(this::compareStableOrder)
                .toList();
        if (eligibleTokens.isEmpty()) {
            return null;
        }
        if (eligibleTokens.size() == 1 || provider.getId() == null) {
            return eligibleTokens.get(0);
        }
        int startIndex = Math.floorMod(distributedCursor(provider), eligibleTokens.size());
        return eligibleTokens.get(startIndex);
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

    private int distributedCursor(Provider provider) {
        long cursor = distributedRouteCacheRepository.nextProviderTokenCursor(provider.getId());
        return cursor <= 0L ? 0 : (int) (cursor - 1L);
    }

    private int compareStableOrder(ProviderToken left, ProviderToken right) {
        int byId = Long.compare(tokenId(left), tokenId(right));
        if (byId != 0) {
            return byId;
        }
        return Objects.toString(left == null ? null : left.getTokenName(), "")
                .compareTo(Objects.toString(right == null ? null : right.getTokenName(), ""));
    }

    private long tokenId(ProviderToken token) {
        return token == null || token.getId() == null ? Long.MAX_VALUE : token.getId();
    }
}
