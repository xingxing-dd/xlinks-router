package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer token update request.
 */
@Data
@Schema(description = "Customer token update request")
public class CustomerTokenUpdateDTO {

    @Schema(description = "Customer account identifier: username, phone, or email")
    private String customerName;

    @Schema(description = "Token name")
    private String tokenName;

    @Schema(description = "Expire time")
    private LocalDateTime expireTime;

    @Schema(description = "Allowed model codes in JSON array string")
    private String allowedModels;

    @Schema(description = "Daily usage quota, NULL means unlimited")
    private BigDecimal dailyQuota;

    @Schema(description = "Total usage quota, NULL means unlimited")
    private BigDecimal totalQuota;

    @Schema(description = "Remark")
    private String remark;
}
