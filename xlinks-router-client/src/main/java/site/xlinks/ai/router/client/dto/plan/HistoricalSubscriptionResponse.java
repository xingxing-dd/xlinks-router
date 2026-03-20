package site.xlinks.ai.router.client.dto.plan;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 历史订阅返回
 */
@Data
public class HistoricalSubscriptionResponse {
    private String id;
    private String planId;
    private String planName;
    private String purchaseDate;
    private String expiryDate;
    private BigDecimal totalQuota;
    private BigDecimal usedQuota;
    private Integer usedPercentage;
    private String status;
}
