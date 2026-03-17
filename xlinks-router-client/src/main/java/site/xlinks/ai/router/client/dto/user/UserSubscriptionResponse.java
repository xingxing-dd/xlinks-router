package site.xlinks.ai.router.client.dto.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserSubscriptionResponse {
    private String planId;
    private String planName;
    private BigDecimal dailyLimit;
    private BigDecimal dailyUsed;
    private BigDecimal monthlyQuota;
    private BigDecimal monthlyUsed;
    private Integer concurrency;
    private Integer currentConcurrency;
    private String expireTime;
    private String status;
}
