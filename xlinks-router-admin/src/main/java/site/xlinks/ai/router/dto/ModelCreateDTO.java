package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Standard model create request.
 */
@Data
@Schema(description = "Standard model create request")
public class ModelCreateDTO {

    @Schema(description = "Model name", required = true)
    @NotBlank(message = "Model name must not be blank")
    private String modelName;

    @Schema(description = "Model code", required = true)
    @NotBlank(message = "Model code must not be blank")
    private String modelCode;

    @Schema(description = "Model description")
    private String modelDesc;

    @Schema(description = "Platform input price")
    private BigDecimal inputPrice;

    @Schema(description = "Platform output price")
    private BigDecimal outputPrice;

    @Schema(description = "Platform cache-hit input price")
    private BigDecimal cacheHitPrice;

    @Schema(description = "Context window")
    private Integer contextSize;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Remark")
    private String remark;
}
