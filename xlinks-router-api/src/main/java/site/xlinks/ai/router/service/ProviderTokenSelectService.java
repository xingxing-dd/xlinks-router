package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;
import site.xlinks.ai.router.service.routing.ProxyErrors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Selects available provider tokens under a provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTokenSelectService {

    private final ProviderTokenMapper providerTokenMapper;
    private final RouteCacheService routeCacheService;

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
        log.debug("Selecting token for provider: {}", providerId);
        List<ProviderToken> tokens = routeCacheService.getProviderTokens(providerId);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        ProviderToken selected = null;
        for (ProviderToken token : tokens) {
            if (!isAvailable(token, now)) {
                continue;
            }
            if (isBetterCandidate(token, selected)) {
                selected = token;
            }
        }
        if (selected == null) {
            return null;
        }

        // Keep routing hot path read-heavy: update in-memory last-used instead of DB per request.
        selected.setLastUsedAt(now);
        routeCacheService.touchProviderToken(selected.getId(), now);
        log.debug("Selected token: {} for provider: {}", selected.getId(), providerId);
        return selected;
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

    private boolean isBetterCandidate(ProviderToken candidate, ProviderToken currentBest) {
        if (candidate == null) {
            return false;
        }
        if (currentBest == null) {
            return true;
        }

        long candidateRemaining = remainingQuota(candidate);
        long bestRemaining = remainingQuota(currentBest);
        if (candidateRemaining != bestRemaining) {
            return candidateRemaining > bestRemaining;
        }

        int lastUsedCompare = compareLastUsed(candidate.getLastUsedAt(), currentBest.getLastUsedAt());
        if (lastUsedCompare != 0) {
            return lastUsedCompare < 0;
        }

        return normalizeId(candidate.getId()) < normalizeId(currentBest.getId());
    }

    private int compareLastUsed(LocalDateTime candidate, LocalDateTime best) {
        if (candidate == null && best == null) {
            return 0;
        }
        if (candidate == null) {
            return -1;
        }
        if (best == null) {
            return 1;
        }
        return candidate.compareTo(best);
    }

    private long normalizeId(Long id) {
        return id == null ? Long.MAX_VALUE : id;
    }
}
