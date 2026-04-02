package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Provider model mapping create request.
 */
@Data
@Schema(description = "Provider model mapping create request")
public class ProviderModelCreateDTO {

    @NotNull
    @Schema(description = "Provider ID", required = true)
    private Long providerId;

    @NotNull
    @Schema(description = "Standard model ID", required = true)
    private Long modelId;

    @NotBlank
    @Schema(description = "Upstream provider model code", required = true)
    private String providerModelCode;

    @Schema(description = "Upstream provider model name")
    private String providerModelName;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Remark")
    private String remark;
}
