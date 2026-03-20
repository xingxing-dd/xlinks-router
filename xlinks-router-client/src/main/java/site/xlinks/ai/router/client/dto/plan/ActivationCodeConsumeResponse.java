package site.xlinks.ai.router.client.dto.plan;

import lombok.Data;

/**
 * 激活码兑换响应
 */
@Data
public class ActivationCodeConsumeResponse {
    private String message;
    private String activatedPlanId;
    private String activatedPlanName;
    private String expireTime;
    private String subscriptionId;
}
