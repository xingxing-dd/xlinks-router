package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects available provider tokens under a provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTokenSelectService {

    private final ProviderTokenMapper providerTokenMapper;
    private final RouteCacheService routeCacheService;

    @Transactional
    public ProviderToken selectToken(Long providerId) {
        ProviderToken selected = selectTokenOrNull(providerId);
        if (selected == null) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "No available Provider Token");
        }
        return selected;
    }

    @Transactional
    public ProviderToken selectTokenOrNull(Long providerId) {
        log.debug("Selecting token for provider: {}", providerId);
        List<ProviderToken> tokens = routeCacheService.getProviderTokens(providerId);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        List<ProviderToken> availableTokens = tokens.stream()
                .filter(this::isAvailable)
                .collect(Collectors.toList());
        if (availableTokens.isEmpty()) {
            return null;
        }

        ProviderToken selected = availableTokens.stream()
                .sorted(Comparator
                        .comparingLong(this::remainingQuota).reversed()
                        .thenComparing(ProviderToken::getLastUsedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(token -> token.getId() == null ? Long.MAX_VALUE : token.getId()))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return null;
        }

        // Keep routing hot path read-heavy: update in-memory last-used instead of DB per request.
        LocalDateTime now = LocalDateTime.now();
        selected.setLastUsedAt(now);
        routeCacheService.touchProviderToken(selected.getId(), now);
        log.debug("Selected token: {} for provider: {}", selected.getId(), providerId);
        return selected;
    }

    private boolean isAvailable(ProviderToken token) {
        if (token == null) {
            return false;
        }
        if (token.getTokenStatus() == null || token.getTokenStatus() != 1) {
            return false;
        }
        LocalDateTime expireTime = token.getExpireTime();
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            return false;
        }
        Long quotaTotal = token.getQuotaTotal();
        Long quotaUsed = token.getQuotaUsed();
        return quotaTotal == null || quotaUsed == null || quotaUsed < quotaTotal;
    }

    @Transactional
    public void updateQuotaUsed(Long tokenId, Long usedTokens) {
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
}
