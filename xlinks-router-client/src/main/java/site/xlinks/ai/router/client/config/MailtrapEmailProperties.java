package site.xlinks.ai.router.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mailtrap mail service properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "xlinks.email.mailtrap")
public class MailtrapEmailProperties {

    /**
     * API access token.
     */
    private String accessToken;

    /**
     * Sender email address.
     */
    private String senderEmail;

    /**
     * Default sender display name.
     */
    private String senderName;

    /**
     * Sender name template. Supported placeholders: {appName}, {scene}, {sceneLabel}.
     */
    private String senderNameTemplate = "{appName}";

    /**
     * Verify-code email subject template. Supported placeholders: {appName}, {scene}, {sceneLabel}.
     */
    private String verifyCodeSubjectTemplate = "[{appName}] {sceneLabel} verification code";

    /**
     * Application name.
     */
    private String appName = "xlinks";

    /**
     * Mailtrap send API URL.
     */
    private String apiUrl = "https://send.api.mailtrap.io/api/send";

    /**
     * Mail category.
     */
    private String category = "Integration Test";

    /**
     * Whether enabled.
     */
    private boolean enabled = true;
}