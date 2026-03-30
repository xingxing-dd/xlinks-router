package site.xlinks.ai.router.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Brevo邮件服务配置
 * Brevo (formerly Sendinblue) SMTP API配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "xlinks.email.brevo")
public class BrevoEmailProperties {

    /**
     * API密钥
     */
    private String accessToken;

    /**
     * 发件人邮箱
     */
    private String senderEmail;

    /**
     * 发件人名称
     */
    private String senderName;

    /**
     * API地址
     */
    private String apiUrl = "https://api.brevo.com/v3/smtp/email";

    /**
     * 是否启用
     */
    private boolean enabled = true;
}