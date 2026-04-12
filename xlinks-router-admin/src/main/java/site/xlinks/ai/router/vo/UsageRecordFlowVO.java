package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Usage record flow view object")
public class UsageRecordFlowVO {

    private Long id;
    private String requestId;
    private Long accountId;
    private String accountName;
    private String accountPhone;
    private String accountEmail;
    private String customerToken;
    private String providerToken;
    private String usageType;
    private String usageFrom;
    private Long providerId;
    private String providerCode;
    private String providerName;
    private String endpointCode;
    private Long modelId;
    private String modelCode;
    private String modelName;
    private Integer responseStatus;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer cacheHitTokens;
    private BigDecimal promptCost;
    private BigDecimal cacheHitCost;
    private BigDecimal completionCost;
    private BigDecimal totalCost;
    private Integer responseMs;
    private Integer sessionMs;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
}
