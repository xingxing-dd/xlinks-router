package site.xlinks.ai.router.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.service.routing.ProxyErrors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Customer token validation and permission checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenAuthService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final RouteCacheService routeCacheService;
    private final CustomerTokenMapper customerTokenMapper;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<String>> allowedModelsCache = new ConcurrentHashMap<>();

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
        CustomerToken freshToken = loadFreshCustomerToken(cachedToken, token);
        assertRequestAllowed(freshToken, model, now);
        routeCacheService.cacheCustomerToken(freshToken);
        return freshToken;
    }

    /**
     * Check whether a customer token can access given model.
     */
    public boolean hasPermissionForModel(CustomerToken customerToken, String model) {
        String allowedModels = customerToken == null ? null : customerToken.getAllowedModels();
        return isModelAllowed(allowedModels, model);
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

    private void assertRequestAllowed(CustomerToken customerToken, String model, LocalDateTime now) {
        assertTokenState(customerToken, now);
        assertModelAllowed(customerToken.getAllowedModels(), model);
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

    private void assertModelAllowed(String allowedModels, String model) {
        if (model == null || model.isBlank()) {
            return;
        }
        if (!isModelAllowed(allowedModels, model)) {
            throw ProxyErrors.customerTokenModelNotAllowed();
        }
    }

    private boolean isModelAllowed(String allowedModels, String model) {
        if (allowedModels == null || allowedModels.isBlank()) {
            return true;
        }

        try {
            Set<String> allowedSet = allowedModelsCache.computeIfAbsent(allowedModels, this::parseAllowedModelsToSet);
            return allowedSet.contains(model);
        } catch (Exception e) {
            log.warn("Failed to parse allowed models, granting access", e);
            return true;
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

    private Set<String> parseAllowedModelsToSet(String allowedModels) {
        if (allowedModels == null || allowedModels.isBlank()) {
            return Collections.emptySet();
        }

        if (allowedModels.trim().startsWith("[")) {
            try {
                List<String> models = objectMapper.readValue(allowedModels, STRING_LIST_TYPE);
                if (models == null || models.isEmpty()) {
                    return Collections.emptySet();
                }
                return models.stream()
                        .filter(item -> item != null && !item.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid allowedModels JSON", e);
            }
        }

        return Arrays.stream(allowedModels.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
