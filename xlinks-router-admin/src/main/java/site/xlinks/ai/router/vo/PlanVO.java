package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plan view object.
 */
@Data
@Schema(description = "Plan view object")
public class PlanVO {

    private Long id;

    private String planName;

    private BigDecimal price;

    private Integer durationDays;

    private BigDecimal dailyQuota;

    private BigDecimal totalQuota;

    private BigDecimal multiplier;

    private Integer maxPurchaseCount;

    private String allowedModels;

    private Integer status;

    private Integer visible;

    private String payUrl;

    private Integer payLinkStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
