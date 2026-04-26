package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.service.routing.ProxyErrors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Customer token validation and permission checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenAuthService {

    private final RouteCacheService routeCacheService;
    private final CustomerTokenMapper customerTokenMapper;
    private final ConcurrentMap<Long, FreshTokenSnapshot> freshTokenSnapshotCache = new ConcurrentHashMap<>();

    @Value("${xlinks.router.auth.token-fresh-verify-ttl-ms:1000}")
    private long tokenFreshVerifyTtlMs;

    /**
     * Validate customer bearer token state only.
     */
    public CustomerToken validateToken(String token) {
        LocalDateTime now = LocalDateTime.now();
        CustomerToken customerToken = loadCustomerToken(token);
        assertTokenState(customerToken, now);
        log.debug("Token validated for customer: {}", customerToken.getCustomerName());
        return customerToken;
    }

    /**
     * Validate token access for a specific model.
     */
    public CustomerToken validateRequestAccess(String token, String model) {
        LocalDateTime now = LocalDateTime.now();
        CustomerToken cachedToken = loadCustomerToken(token);
        assertRequestAllowed(cachedToken, model, now);
        CustomerToken freshToken = resolveFreshCustomerToken(cachedToken, token);
        assertRequestAllowed(freshToken, model, now);
        if (freshToken != cachedToken) {
            routeCacheService.cacheCustomerToken(freshToken);
        }
        return freshToken;
    }

    /**
     * Check whether a customer token can access given model.
     */
    public boolean hasPermissionForModel(CustomerToken customerToken, String model) {
        return routeCacheService.isCustomerTokenModelAllowed(customerToken, model);
    }

    private CustomerToken loadCustomerToken(String token) {
        if (token == null || token.isBlank()) {
            throw ProxyErrors.missingBearerToken();
        }
        CustomerToken customerToken = routeCacheService.getCustomerTokenByValue(token);
        if (customerToken == null) {
            throw ProxyErrors.invalidToken();
        }
        return customerToken;
    }

    private CustomerToken loadFreshCustomerToken(CustomerToken cachedToken, String token) {
        if (cachedToken == null || cachedToken.getId() == null) {
            throw ProxyErrors.invalidToken();
        }
        CustomerToken freshToken = customerTokenMapper.selectById(cachedToken.getId());
        if (freshToken == null || freshToken.getTokenValue() == null || !freshToken.getTokenValue().equals(token)) {
            throw ProxyErrors.invalidToken();
        }
        return freshToken;
    }

    private CustomerToken resolveFreshCustomerToken(CustomerToken cachedToken, String token) {
        if (cachedToken == null || cachedToken.getId() == null) {
            throw ProxyErrors.invalidToken();
        }
        long ttlMs = Math.max(tokenFreshVerifyTtlMs, 0L);
        if (ttlMs > 0) {
            FreshTokenSnapshot snapshot = freshTokenSnapshotCache.get(cachedToken.getId());
            long nowMs = System.currentTimeMillis();
            if (snapshot != null
                    && snapshot.expiresAtMs() >= nowMs
                    && snapshot.customerToken() != null
                    && token.equals(snapshot.customerToken().getTokenValue())) {
                return snapshot.customerToken();
            }
        }
        CustomerToken freshToken = loadFreshCustomerToken(cachedToken, token);
        if (ttlMs > 0) {
            freshTokenSnapshotCache.put(
                    freshToken.getId(),
                    new FreshTokenSnapshot(freshToken, System.currentTimeMillis() + ttlMs)
            );
        }
        return freshToken;
    }

    private void assertRequestAllowed(CustomerToken customerToken, String model, LocalDateTime now) {
        assertTokenState(customerToken, now);
        assertModelAllowed(customerToken, model);
        assertQuotaAvailable(
                customerToken.getDailyQuota(),
                customerToken.getUsedQuota(),
                customerToken.getTotalQuota(),
                customerToken.getTotalUsedQuota()
        );
    }

    private void assertQuotaAvailable(BigDecimal dailyQuota,
                                      BigDecimal dailyUsedQuota,
                                      BigDecimal totalQuota,
                                      BigDecimal totalUsedQuota) {
        if (totalQuota != null && totalQuota.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usedTotal = defaultDecimal(totalUsedQuota);
            if (usedTotal.compareTo(totalQuota) >= 0) {
                throw ProxyErrors.customerTokenTotalQuotaReached();
            }
        }

        if (dailyQuota == null || dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (defaultDecimal(dailyUsedQuota).compareTo(dailyQuota) >= 0) {
            throw ProxyErrors.customerTokenDailyQuotaReached();
        }
    }

    private void assertModelAllowed(CustomerToken customerToken, String model) {
        if (model == null || model.isBlank()) {
            return;
        }
        if (!routeCacheService.isCustomerTokenModelAllowed(customerToken, model)) {
            throw ProxyErrors.customerTokenModelNotAllowed();
        }
    }

    private void assertTokenState(CustomerToken customerToken, LocalDateTime now) {
        assertTokenState(customerToken == null ? null : customerToken.getStatus(),
                customerToken == null ? null : customerToken.getExpireTime(),
                now);
    }

    private void assertTokenState(Integer status, LocalDateTime expireTime, LocalDateTime now) {
        if (status == null || status != 1) {
            throw ProxyErrors.tokenDisabled();
        }
        if (expireTime != null && now.isAfter(expireTime)) {
            throw ProxyErrors.tokenExpired();
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record FreshTokenSnapshot(CustomerToken customerToken, long expiresAtMs) {
    }
}
