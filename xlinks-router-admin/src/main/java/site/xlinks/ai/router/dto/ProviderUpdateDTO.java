package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Provider update request.
 */
@Data
@Schema(description = "Provider update request")
public class ProviderUpdateDTO {

    @Schema(description = "Provider name")
    private String providerName;

    @Schema(description = "Base URL")
    private String baseUrl;

    @Schema(description = "Supported request protocols, comma-separated, e.g. chat/completions,responses")
    private String supportedProtocols;

    @Schema(description = "Route priority, higher value means higher priority")
    private Integer priority;

    @Schema(description = "Provider logo URL")
    private String providerLogo;

    @Schema(description = "Provider website URL")
    private String providerWebsite;

    @Schema(description = "Whether provider-token concurrency limit is enabled: 1-enabled, 0-disabled")
    private Integer concurrencyLimitEnabled;

    @Schema(description = "Max concurrent sessions allowed per provider token")
    private Integer maxConcurrentPerToken;

    @Schema(description = "Wait time when acquiring permit in milliseconds")
    private Integer acquireTimeoutMs;

    @Schema(description = "Non-stream request timeout in milliseconds")
    private Integer requestTimeoutMs;

    @Schema(description = "Stream first response timeout in milliseconds")
    private Integer streamFirstResponseTimeoutMs;

    @Schema(description = "Stream idle timeout in milliseconds")
    private Integer streamIdleTimeoutMs;

    @Schema(description = "Permit lease duration in milliseconds")
    private Integer sessionLeaseMs;

    @Schema(description = "Permit renew interval in milliseconds")
    private Integer sessionRenewIntervalMs;

    @Schema(description = "Remark")
    private String remark;
}
