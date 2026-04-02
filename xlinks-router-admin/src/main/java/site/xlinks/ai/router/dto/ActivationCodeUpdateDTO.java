package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Activation code update request.
 */
@Data
@Schema(description = "Activation code update request")
public class ActivationCodeUpdateDTO {

    @Schema(description = "Plan ID")
    private Long planId;

    @Schema(description = "Order ID")
    private String orderId;

    @Schema(description = "Remark")
    private String remark;
}
