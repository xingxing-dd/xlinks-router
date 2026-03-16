package site.xlinks.ai.router.merchant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证模块配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String smsCodePrefix;

    private Long smsCodeExpireSeconds;

    private Long smsSendIntervalSeconds;

    private String authTokenPrefix;

    private Long authTokenExpireSeconds;

    private String jwtSecret;

    private String jwtIssuer;

    private Integer rsaKeySize = 2048;

    private String rsaPublicKey;

    private String rsaPrivateKey;

    private Boolean smsMockEnabled = Boolean.TRUE;
}