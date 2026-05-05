package site.xlinks.ai.router.app.forwarding.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ForwardingUsageContext {

    private String requestId;

    private Long accountId;

    private Long customerTokenId;

    private String customerTokenValue;

    private Long planId;

    private Long providerId;

    private String providerCode;

    private String providerName;

    private Long providerTokenId;

    private String providerTokenName;

    private String providerTokenValue;

    private String endpointCode;

    private Long modelId;

    private String modelCode;

    private String modelName;

    private String modelProvider;

    private BigDecimal inputPrice;

    private BigDecimal cacheHitPrice;

    private BigDecimal outputPrice;

    private BigDecimal multiplier;
}
