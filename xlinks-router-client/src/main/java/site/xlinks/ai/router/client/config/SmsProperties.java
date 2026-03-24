package site.xlinks.ai.router.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMS properties for verify-code + provider config.
 */
@Data
@ConfigurationProperties(prefix = "xlinks.sms")
public class SmsProperties {

    private VerifyCode verifyCode = new VerifyCode();
    private Guoyangyun guoyangyun = new Guoyangyun();

    @Data
    public static class VerifyCode {
        private int expireSeconds = 300;
        private int dailyLimit = 20;
        private int minIntervalSeconds = 60;
    }

    @Data
    public static class Guoyangyun {
        private String host;
        private String path;
        private String appcode;
        private String smsSignId;
        private String templateId;
    }
}
