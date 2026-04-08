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

    @Schema(description = "Cache-hit strategy: none/openai_cached_tokens/anthropic_cache_read_input_tokens")
    private String cacheHitStrategy;

    @Schema(description = "Provider logo URL")
    private String providerLogo;

    @Schema(description = "Provider website URL")
    private String providerWebsite;

    @Schema(description = "Remark")
    private String remark;
}
