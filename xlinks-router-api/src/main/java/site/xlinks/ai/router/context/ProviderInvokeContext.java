package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

/**
 * Provider 调用上下文
 * 包含调用下游 Provider 所需的完整信息
 */
@Data
@Builder
public class ProviderInvokeContext {

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * Provider ID
     */
    private Long providerId;

    /**
     * Provider 编码
     */
    private String providerCode;

    /**
     * Provider 名称
     */
    private String providerName;

    /**
     * Provider 类型
     */
    private String providerType;

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * Provider Token
     */
    private String providerToken;

    /**
     * 客户 Token
     */
    private String customerToken;

    /**
     * 端点编码
     */
    private String endpointCode;

    /**
     * 模型 ID
     */
    private Long modelId;

    /**
     * 模型编码
     */
    private String modelCode;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 输入价格（每百万 token）
     */
    private java.math.BigDecimal inputPrice;

    /**
     * 输出价格（每百万 token）
     */
    private java.math.BigDecimal outputPrice;

    /**
     * 底层模型名称
     */
    private String providerModel;

    /**
     * 套餐记录 ID
     */
    private Long planId;

    /**
     * 客户 Token ID
     */
    private Long customerTokenId;

    /**
     * 客户账户 ID
     */
    private Long accountId;

    /**
     * 客户模型编码
     */
    private String customerModel;
}
