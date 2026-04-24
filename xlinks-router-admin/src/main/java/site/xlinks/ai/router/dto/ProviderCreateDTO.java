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

    @Schema(description = "Whether provider-token concurrency limit is enabled: 1-enabled, 0-disabled")
    private Integer concurrencyLimitEnabled = 0;

    @Schema(description = "Max concurrent sessions allowed per provider token")
    private Integer maxConcurrentPerToken = 0;

    @Schema(description = "Wait time when acquiring permit in milliseconds")
    private Integer acquireTimeoutMs = 0;

    @Schema(description = "Non-stream request timeout in milliseconds")
    private Integer requestTimeoutMs = 20000;

    @Schema(description = "Stream first response timeout in milliseconds")
    private Integer streamFirstResponseTimeoutMs = 20000;

    @Schema(description = "Stream idle timeout in milliseconds")
    private Integer streamIdleTimeoutMs = 20000;

    @Schema(description = "Permit lease duration in milliseconds")
    private Integer sessionLeaseMs = 30000;

    @Schema(description = "Permit renew interval in milliseconds")
    private Integer sessionRenewIntervalMs = 10000;

    @Schema(description = "Remark")
    private String remark;
}
