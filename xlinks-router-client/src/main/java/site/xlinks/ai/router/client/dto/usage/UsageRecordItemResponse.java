package site.xlinks.ai.router.client.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordItemResponse {
    private Long id;
    private String requestId;
    private String providerCode;
    private String providerName;
    private String endpointCode;
    private String modelCode;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer cacheHitTokens;
    private BigDecimal promptCost;
    private BigDecimal cacheHitCost;
    private BigDecimal completionCost;
    private BigDecimal totalCost;
    private Integer latencyMs;
    private Integer responseStatus;
    private String errorCode;
    private String createdAt;
}
