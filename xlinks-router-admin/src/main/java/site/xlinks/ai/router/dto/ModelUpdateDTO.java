package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Standard model update request.
 */
@Data
@Schema(description = "Standard model update request")
public class ModelUpdateDTO {

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "Model vendor label for display, e.g. OPENAI / ANTHROPIC")
    private String modelProvider;

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

    @Schema(description = "Remark")
    private String remark;
}
