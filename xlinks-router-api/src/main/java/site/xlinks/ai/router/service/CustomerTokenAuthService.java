package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;

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

    private final CustomerTokenMapper customerTokenMapper;
    private final ObjectMapper objectMapper;

    @Value("${xlinks.router.auth.token-cache-ttl-seconds:15}")
    private long tokenCacheTtlSeconds;

    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> allowedModelsCache = new ConcurrentHashMap<>();

    /**
     * Validate customer bearer token.
     */
    public CustomerToken validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing bearer token");
        }

        LocalDateTime now = LocalDateTime.now();
        CachedToken cached = tokenCache.get(token);
        if (cached != null && cached.isValidAt(now)) {
            return cached.customerToken();
        }

        CustomerToken customerToken = customerTokenMapper.selectOne(
                new LambdaQueryWrapper<CustomerToken>().eq(CustomerToken::getTokenValue, token)
        );
        if (customerToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token");
        }
        if (customerToken.getStatus() == null || customerToken.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token is disabled");
        }
        if (customerToken.getExpireTime() != null && now.isAfter(customerToken.getExpireTime())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token has expired");
        }

        cacheValidatedToken(token, customerToken, now);
        log.debug("Token validated for customer: {}", customerToken.getCustomerName());
        return customerToken;
    }

    /**
     * Check whether a customer token can access given model.
     */
    public boolean hasPermissionForModel(CustomerToken customerToken, String model) {
        String allowedModels = customerToken == null ? null : customerToken.getAllowedModels();
        if (allowedModels == null || allowedModels.isBlank()) {
            return true;
        }

        try {
            Set<String> allowedSet = allowedModelsCache.computeIfAbsent(allowedModels, this::parseAllowedModelsToSet);
            return allowedSet.contains(model);
        } catch (Exception e) {
            // Keep backward-compatible fail-open behavior on malformed data.
            log.warn("Failed to parse allowed models, granting access", e);
            return true;
        }
    }

    private void cacheValidatedToken(String token, CustomerToken customerToken, LocalDateTime now) {
        LocalDateTime cacheExpireAt = now.plusSeconds(Math.max(tokenCacheTtlSeconds, 1));
        LocalDateTime tokenExpireAt = customerToken.getExpireTime();
        if (tokenExpireAt != null && tokenExpireAt.isBefore(cacheExpireAt)) {
            cacheExpireAt = tokenExpireAt;
        }
        tokenCache.put(token, new CachedToken(customerToken, cacheExpireAt));
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

    private record CachedToken(CustomerToken customerToken, LocalDateTime expireAt) {
        boolean isValidAt(LocalDateTime now) {
            return true;//expireAt != null && !now.isAfter(expireAt);
        }
    }
}
