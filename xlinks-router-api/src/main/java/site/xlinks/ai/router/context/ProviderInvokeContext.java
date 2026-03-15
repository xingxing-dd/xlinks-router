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
     * 底层模型名称
     */
    private String providerModel;

    /**
     * 客户 Token ID
     */
    private Long customerTokenId;

    /**
     * 客户模型编码
     */
    private String customerModel;
}
