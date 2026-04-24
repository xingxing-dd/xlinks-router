package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;
import site.xlinks.ai.router.service.routing.ProxyErrors;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Selects available provider tokens and acquires concurrency permits when needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTokenSelectService {

    private final ProviderTokenMapper providerTokenMapper;
    private final RouteCacheService routeCacheService;
    private final ProviderConcurrencyGuard providerConcurrencyGuard;

    public ProviderToken selectToken(Long providerId) {
        ProviderToken selected = selectTokenOrNull(providerId);
        if (selected == null) {
            throw ProxyErrors.noProviderTokenAvailable();
        }
        return selected;
    }

    public ProviderToken selectTokenOrNull(Long providerId) {
        if (providerId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return listAvailableTokens(providerId, now).stream().findFirst().orElse(null);
    }

    public SelectionResult selectTokenLeaseOrNull(Provider provider, String requestId) {
        if (provider == null || provider.getId() == null) {
            return SelectionResult.none(false);
        }
        log.debug("Selecting token lease for provider: {}, requestId={}", provider.getId(), requestId);

        LocalDateTime now = LocalDateTime.now();
        List<ProviderToken> candidates = listAvailableTokens(provider.getId(), now);
        if (candidates.isEmpty()) {
            return SelectionResult.none(false);
        }

        boolean concurrencyLimited = false;
        for (ProviderToken token : candidates) {
            ProviderPermitLease lease = providerConcurrencyGuard.tryAcquire(provider, token, requestId);
            if (lease == null) {
                concurrencyLimited = true;
                continue;
            }
            token.setLastUsedAt(now);
            routeCacheService.touchProviderToken(token.getId(), now);
            log.debug("Selected token: {} for provider: {}, requestId={}", token.getId(), provider.getId(), requestId);
            return new SelectionResult(token, lease, false);
        }
        return SelectionResult.none(concurrencyLimited);
    }

    @Transactional
    public void updateQuotaUsed(Long tokenId, Long usedTokens) {
        if (tokenId == null || usedTokens == null || usedTokens <= 0) {
            return;
        }
        ProviderToken token = providerTokenMapper.selectById(tokenId);
        if (token == null) {
            return;
        }
        Long currentUsed = token.getQuotaUsed();
        if (currentUsed == null) {
            currentUsed = 0L;
        }
        token.setQuotaUsed(currentUsed + usedTokens);
        providerTokenMapper.updateById(token);
        routeCacheService.updateProviderTokenQuota(tokenId, token.getQuotaUsed());
    }

    private List<ProviderToken> listAvailableTokens(Long providerId, LocalDateTime now) {
        List<ProviderToken> tokens = routeCacheService.getProviderTokens(providerId);
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokens.stream()
                .filter(token -> isAvailable(token, now))
                .sorted(tokenComparator())
                .toList();
    }

    private boolean isAvailable(ProviderToken token, LocalDateTime now) {
        if (token == null) {
            return false;
        }
        if (token.getTokenStatus() == null || token.getTokenStatus() != 1) {
            return false;
        }
        LocalDateTime expireTime = token.getExpireTime();
        if (expireTime != null && now.isAfter(expireTime)) {
            return false;
        }
        Long quotaTotal = token.getQuotaTotal();
        Long quotaUsed = token.getQuotaUsed();
        return quotaTotal == null || quotaUsed == null || quotaUsed < quotaTotal;
    }

    private Comparator<ProviderToken> tokenComparator() {
        return Comparator
                .comparingLong(this::remainingQuota)
                .reversed()
                .thenComparing(ProviderToken::getLastUsedAt, this::compareLastUsed)
                .thenComparing(token -> normalizeId(token.getId()));
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

    private long normalizeId(Long id) {
        return id == null ? Long.MAX_VALUE : id;
    }

    public record SelectionResult(ProviderToken token, ProviderPermitLease lease, boolean concurrencyLimited) {
        static SelectionResult none(boolean concurrencyLimited) {
            return new SelectionResult(null, null, concurrencyLimited);
        }
    }
}
