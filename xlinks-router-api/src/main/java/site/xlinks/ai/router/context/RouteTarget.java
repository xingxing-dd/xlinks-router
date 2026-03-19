package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

/**
 * 路由目标
 * 表示一次请求应该路由到的目标 Provider 和模型
 */
@Data
@Builder
public class RouteTarget {

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
     * 底层模型名称
     */
    private String providerModel;

    /**
     * 模型 ID
     */
    private Long modelId;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 使用类型：0-不限制，1-仅套餐，2-仅余额
     */
    private Integer usageType;

    /**
     * 模型类型：chat、embedding、image 等
     */
    private String modelType;
}
