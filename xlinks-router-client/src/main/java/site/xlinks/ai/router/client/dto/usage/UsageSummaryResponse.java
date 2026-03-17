package site.xlinks.ai.router.client.dto.usage;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UsageSummaryResponse {
    private Integer totalRequests;
    private Integer totalTokens;
    private BigDecimal totalCost;
    private Integer avgLatencyMs;
    private Double successRate;
    private List<UsageModelStatResponse> modelStats;
    private List<UsageProviderStatResponse> providerStats;
}
