package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Admin grant subscription request.
 */
@Data
@Schema(description = "Admin grant subscription request")
public class SubscriptionGrantDTO {

    @Schema(description = "Merchant account ID", required = true)
    @NotNull(message = "Account ID must not be null")
    private Long accountId;

    @Schema(description = "Plan ID", required = true)
    @NotNull(message = "Plan ID must not be null")
    private Long planId;
}
