package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Provider model mapping batch create request.
 */
@Data
@Schema(description = "Provider model mapping batch create request")
public class ProviderModelBatchCreateDTO {

    @NotNull
    @Schema(description = "Provider ID", required = true)
    private Long providerId;

    @NotEmpty
    @Schema(description = "Standard model IDs", required = true)
    private List<Long> modelIds;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Remark")
    private String remark;
}

