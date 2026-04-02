package site.xlinks.ai.router.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.config.AdminAuthProperties;
import site.xlinks.ai.router.context.AdminAccountContext;
import site.xlinks.ai.router.entity.AdminAccount;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Admin token service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenService {

    private static final String TOKEN_PREFIX = "admin:token:";

    private final StringRedisTemplate redisTemplate;
    private final AdminAuthProperties adminAuthProperties;

    public String generateToken(AdminAccount account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", account.getUsername());
        claims.put("displayName", account.getDisplayName());
        claims.put("email", account.getEmail());
        claims.put("phone", account.getPhone());

        SecretKey key = Keys.hmacShaKeyFor(adminAuthProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(account.getId()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + adminAuthProperties.getTokenExpireSeconds() * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        redisTemplate.opsForValue().set(buildRedisKey(account.getId()), token, adminAuthProperties.getTokenExpireSeconds(), TimeUnit.SECONDS);
        return token;
    }

    public AdminAccount validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(adminAuthProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
            Long accountId = Long.parseLong(claims.getSubject());
            String cachedToken = redisTemplate.opsForValue().get(buildRedisKey(accountId));
            if (cachedToken == null || !cachedToken.equals(token)) {
                return null;
            }
            redisTemplate.expire(buildRedisKey(accountId), adminAuthProperties.getTokenExpireSeconds(), TimeUnit.SECONDS);

            AdminAccount account = new AdminAccount();
            account.setId(accountId);
            account.setUsername(claims.get("username", String.class));
            account.setDisplayName(claims.get("displayName", String.class));
            account.setEmail(claims.get("email", String.class));
            account.setPhone(claims.get("phone", String.class));
            return account;
        } catch (Exception ex) {
            log.warn("Admin token validation failed: {}", ex.getMessage());
            return null;
        }
    }

    public void logout(Long accountId) {
        if (accountId != null) {
            redisTemplate.delete(buildRedisKey(accountId));
        }
        AdminAccountContext.clear();
    }

    public long getExpireSeconds() {
        return adminAuthProperties.getTokenExpireSeconds();
    }

    private String buildRedisKey(Long accountId) {
        return TOKEN_PREFIX + accountId;
    }
}
