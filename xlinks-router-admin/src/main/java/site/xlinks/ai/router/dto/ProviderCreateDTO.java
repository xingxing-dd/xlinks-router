package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Provider create request.
 */
@Data
@Schema(description = "Provider create request")
public class ProviderCreateDTO {

    @Schema(description = "Provider code", required = true)
    @NotBlank(message = "Provider code must not be blank")
    private String providerCode;

    @Schema(description = "Provider name", required = true)
    @NotBlank(message = "Provider name must not be blank")
    private String providerName;

    @Schema(description = "Supported request protocols, comma-separated, e.g. chat/completions,responses")
    private String supportedProtocols;

    @Schema(description = "Route priority, higher value means higher priority")
    private Integer priority = 0;

    @Schema(description = "Base URL", required = true)
    @NotBlank(message = "Base URL must not be blank")
    private String baseUrl;

    @Schema(description = "Provider logo URL")
    private String providerLogo;

    @Schema(description = "Provider website URL")
    private String providerWebsite;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Remark")
    private String remark;
}
