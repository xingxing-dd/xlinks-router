package site.xlinks.ai.router.merchant.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.merchant.config.AuthProperties;
import site.xlinks.ai.router.merchant.entity.AuthUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 登录会话服务。
 */
@Service
public class AuthSessionService {

    private final AuthProperties authProperties;
    private SecretKey jwtSecretKey;

    public AuthSessionService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(authProperties.getJwtSecret()) || authProperties.getJwtSecret().length() < 32) {
            throw new IllegalStateException("app.auth.jwt-secret 至少需要 32 位字符");
        }
        this.jwtSecretKey = Keys.hmacShaKeyFor(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(AuthUser user) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(authProperties.getAuthTokenExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(authProperties.getJwtIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim("tokenType", "merchant-access")
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .signWith(jwtSecretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭证无效或已过期");
        }
    }
}