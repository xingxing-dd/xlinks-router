package site.xlinks.ai.router.client.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.entity.CustomerAccount;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token服务
 * 负责JWT Token的生成、验证和Redis缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.secret:xlinks-router-client-secret-key-must-be-at-least-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:7200}")
    private Long jwtExpiration;

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_PREFIX = "token:";
    private static final long REDIS_EXPIRE_TIME = 7200L; // 2小时

    /**
     * 生成Token
     */
    public String generateToken(CustomerAccount account) {
        // 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", account.getId());
        claims.put("username", account.getUsername());
        claims.put("email", account.getEmail());

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(account.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 缓存到Redis
        String redisKey = TOKEN_PREFIX + account.getId();
        redisTemplate.opsForValue().set(redisKey, token, REDIS_EXPIRE_TIME, TimeUnit.SECONDS);

        log.info("Generated token for account: {}", account.getId());
        return token;
    }

    /**
     * 验证Token
     */
    public CustomerAccount validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String accountIdStr = claims.getSubject();
            Long accountId = Long.parseLong(accountIdStr);

            // 从Redis验证token是否存在
            String redisKey = TOKEN_PREFIX + accountId;
            String cachedToken = redisTemplate.opsForValue().get(redisKey);

            if (cachedToken == null || !cachedToken.equals(token)) {
                log.warn("Token not found in Redis or mismatch, accountId: {}", accountId);
                return null;
            }

            // 刷新Redis过期时间
            redisTemplate.expire(redisKey, REDIS_EXPIRE_TIME, TimeUnit.SECONDS);

            // 返回账户信息
            CustomerAccount account = new CustomerAccount();
            account.setId(accountId);
            account.setUsername(claims.get("username", String.class));
            account.setEmail(claims.get("email", String.class));

            return account;

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从请求中获取当前账户
     */
    public CustomerAccount getCurrentAccount() {
        return CustomerAccountContext.getAccount();
    }

    /**
     * 登出 - 删除Redis中的Token
     */
    public void logout(Long accountId) {
        String redisKey = TOKEN_PREFIX + accountId;
        redisTemplate.delete(redisKey);
        CustomerAccountContext.clear();
        log.info("Logged out, accountId: {}", accountId);
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String token) {
        CustomerAccount account = validateToken(token);
        if (account != null) {
            // 删除旧token
            logout(account.getId());
            // 生成新token
            CustomerAccount fullAccount = new CustomerAccount();
            fullAccount.setId(account.getId());
            fullAccount.setUsername(account.getUsername());
            fullAccount.setEmail(account.getEmail());
            return generateToken(fullAccount);
        }
        return null;
    }
}
