package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Context used during one provider invocation.
 */
@Data
@Builder
public class ProviderInvokeContext {

    private String requestId;

    private Long providerId;

    private String providerCode;

    private String providerName;

    private String baseUrl;

    private String providerToken;

    private String customerToken;

    private String endpointCode;

    private Long modelId;

    private String modelCode;

    private String modelName;

    /**
     * Input price per 1M tokens.
     */
    private BigDecimal inputPrice;

    /**
     * Cache-hit input price per 1M tokens.
     */
    private BigDecimal cacheHitPrice;

    /**
     * Output price per 1M tokens.
     */
    private BigDecimal outputPrice;

    /**
     * Real upstream model provider, e.g. OPENAI / ANTHROPIC.
     */
    private String modelProvider;

    /**
     * Real upstream model code.
     */
    private String providerModel;

    private Long planId;

    private Long customerTokenId;

    private Long accountId;

    /**
     * Customer request model code.
     */
    private String customerModel;
}
