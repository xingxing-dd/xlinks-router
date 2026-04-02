package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Plan create request.
 */
@Data
@Schema(description = "Plan create request")
public class PlanCreateDTO {

    @Schema(description = "Plan name", required = true)
    @NotBlank(message = "Plan name must not be blank")
    private String planName;

    @Schema(description = "Plan price", required = true)
    @NotNull(message = "Plan price must not be null")
    @DecimalMin(value = "0.00", message = "Plan price must not be negative")
    private BigDecimal price;

    @Schema(description = "Duration days", required = true)
    @NotNull(message = "Duration days must not be null")
    @Min(value = 1, message = "Duration days must be at least 1")
    private Integer durationDays;

    @Schema(description = "Daily quota", required = true)
    @NotNull(message = "Daily quota must not be null")
    @DecimalMin(value = "0.00", message = "Daily quota must not be negative")
    private BigDecimal dailyQuota;

    @Schema(description = "Total quota", required = true)
    @NotNull(message = "Total quota must not be null")
    @DecimalMin(value = "0.00", message = "Total quota must not be negative")
    private BigDecimal totalQuota;

    @Schema(description = "Allowed model codes in JSON array format")
    private String allowedModels;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Visible on client side: 1-visible, 0-hidden")
    private Integer visible = 1;

    @Schema(description = "Third-party payment URL")
    private String payUrl;

    @Schema(description = "Payment link status: 1-enabled, 0-disabled")
    private Integer payLinkStatus = 1;

    @Schema(description = "Remark")
    private String remark;
}
