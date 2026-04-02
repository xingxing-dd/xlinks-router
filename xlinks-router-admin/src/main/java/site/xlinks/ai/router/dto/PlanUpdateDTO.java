package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Plan update request.
 */
@Data
@Schema(description = "Plan update request")
public class PlanUpdateDTO {

    @Schema(description = "Plan name")
    private String planName;

    @Schema(description = "Plan price")
    @DecimalMin(value = "0.00", message = "Plan price must not be negative")
    private BigDecimal price;

    @Schema(description = "Duration days")
    @Min(value = 1, message = "Duration days must be at least 1")
    private Integer durationDays;

    @Schema(description = "Daily quota")
    @DecimalMin(value = "0.00", message = "Daily quota must not be negative")
    private BigDecimal dailyQuota;

    @Schema(description = "Total quota")
    @DecimalMin(value = "0.00", message = "Total quota must not be negative")
    private BigDecimal totalQuota;

    @Schema(description = "Allowed model codes in JSON array format")
    private String allowedModels;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status;

    @Schema(description = "Visible on client side: 1-visible, 0-hidden")
    private Integer visible;

    @Schema(description = "Third-party payment URL; pass empty string to remove")
    private String payUrl;

    @Schema(description = "Payment link status: 1-enabled, 0-disabled")
    private Integer payLinkStatus;

    @Schema(description = "Remark")
    private String remark;
}
