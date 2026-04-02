package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Activation code batch generate request.
 */
@Data
@Schema(description = "Activation code batch generate request")
public class ActivationCodeGenerateDTO {

    @Schema(description = "Plan ID", required = true)
    @NotNull(message = "Plan ID must not be null")
    private Long planId;

    @Schema(description = "Generate quantity", required = true)
    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 500, message = "Quantity must not exceed 500")
    private Integer quantity;

    @Schema(description = "Random code length")
    @Min(value = 6, message = "Code length must be at least 6")
    @Max(value = 24, message = "Code length must not exceed 24")
    private Integer codeLength = 12;

    @Schema(description = "Optional code prefix")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "Prefix only supports letters, digits, underscore and hyphen")
    private String prefix;

    @Schema(description = "Remark")
    private String remark;
}
