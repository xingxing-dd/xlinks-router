package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider model mapping batch create result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider model mapping batch create result")
public class ProviderModelBatchCreateVO {

    @Schema(description = "Total requested model count")
    private Integer requestedCount;

    @Schema(description = "Created mapping count")
    private Integer createdCount;

    @Schema(description = "Skipped duplicate mapping count")
    private Integer skippedCount;
}

