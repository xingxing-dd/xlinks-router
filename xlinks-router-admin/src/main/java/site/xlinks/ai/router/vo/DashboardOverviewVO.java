package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Dashboard overview data.
 */
@Data
@Schema(description = "Dashboard overview")
public class DashboardOverviewVO {

    @Schema(description = "Provider count")
    private long providerCount;

    @Schema(description = "Active provider count")
    private long activeProviderCount;

    @Schema(description = "Endpoint count")
    private long endpointCount;

    @Schema(description = "Model count")
    private long modelCount;

    @Schema(description = "Provider model mapping count")
    private long providerModelCount;

    @Schema(description = "Provider token count")
    private long providerTokenCount;

    @Schema(description = "Customer token count")
    private long customerTokenCount;

    @Schema(description = "Tokens expiring within 7 days")
    private long expiringTokenCount;
}
