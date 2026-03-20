package site.xlinks.ai.router.client.dto.plan;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 当前生效订阅返回
 */
@Data
public class ActiveSubscriptionResponse {
    private String id;
    private String planId;
    private String planName;
    private Integer daysRemaining;
    private String purchaseDate;
    private String expiryDate;
    private Boolean dailyReset;
    private BigDecimal remainingQuota;
    private BigDecimal totalQuota;
    private Integer usedPercentage;
}
