package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Usage record summary grouped by model")
public class UsageRecordModelSummaryVO {

    private Long modelId;
    private String modelCode;
    private String modelName;
    private Long requestCount;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long cacheHitTokens;
    private BigDecimal totalCost;
    private BigDecimal avgLatencyMs;
}
