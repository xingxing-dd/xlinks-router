package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Provider model mapping update request.
 */
@Data
@Schema(description = "Provider model mapping update request")
public class ProviderModelUpdateDTO {

    @Schema(description = "Provider ID")
    private Long providerId;

    @Schema(description = "Standard model ID")
    private Long modelId;

    @Schema(description = "Upstream provider model code")
    private String providerModelCode;

    @Schema(description = "Upstream provider model name")
    private String providerModelName;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status;

    @Schema(description = "Remark")
    private String remark;
}
