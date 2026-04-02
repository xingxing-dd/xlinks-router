package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Subscription record view object")
public class SubscriptionRecordVO {

    private Long id;

    private Long accountId;

    private String accountName;

    private String accountPhone;

    private String accountEmail;

    private Integer accountStatus;

    private Long planId;

    private String planName;

    private BigDecimal price;

    private Integer durationDays;

    private BigDecimal dailyQuota;

    private BigDecimal totalQuota;

    private BigDecimal usedQuota;

    private BigDecimal totalUsedQuota;

    private BigDecimal dailyRemainingQuota;

    private BigDecimal totalRemainingQuota;

    private LocalDateTime quotaRefreshTime;

    private LocalDateTime planExpireTime;

    private Integer status;

    private String source;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
