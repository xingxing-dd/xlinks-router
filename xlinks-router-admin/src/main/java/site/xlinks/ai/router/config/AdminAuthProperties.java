package site.xlinks.ai.router.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Admin auth properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.admin-auth")
public class AdminAuthProperties {

    private String jwtSecret = "xlinks-router-admin-secret-key-must-be-at-least-256-bits";

    private long tokenExpireSeconds = 86400L;

    private String bootstrapUsername = "admin";

    private String bootstrapPassword = "admin123";

    private String bootstrapDisplayName = "Super Admin";
}
