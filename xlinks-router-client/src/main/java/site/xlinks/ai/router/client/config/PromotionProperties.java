package site.xlinks.ai.router.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Promotion-related client properties.
 */
@Data
@ConfigurationProperties(prefix = "xlinks.promotion")
public class PromotionProperties {

    /**
     * Referral registration page prefix, e.g. https://token-hub.com/register?ref=
     */
    private String referralLinkPrefix;
}