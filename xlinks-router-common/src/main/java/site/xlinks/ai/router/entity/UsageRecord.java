package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Usage Record 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("usage_records")
public class UsageRecord extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账户 ID
     */
    private Long accountId;

    /**
     * 客户 Token（明文）
     */
    private String customerToken;

    /**
     * Provider Token（明文）
     */
    private String providerToken;

    /**
     * 使用类型：balance/plan
     */
    private String usageType;

    /**
     * 使用来源：套餐 ID/为空
     */
    private String usageFrom;

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
     * Endpoint 编码
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
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 提示词 token 数
     */
    private Integer promptTokens;

    /**
     * 补全 token 数
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    private Integer totalTokens;

    /**
     * 输入 token 费用
     */
    private BigDecimal promptCost;

    /**
     * 输出 token 费用
     */
    private BigDecimal completionCost;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 延迟（毫秒）
     */
    private Integer latencyMs;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间（覆盖父类，usage_records 表无 update_by 等字段）
     */
    private LocalDateTime createdAt;
}
