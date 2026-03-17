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
    private String model;
    private String providerName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer responseStatus;
    private String errorCode;
    private BigDecimal cost;
    private String createdAt;
}
