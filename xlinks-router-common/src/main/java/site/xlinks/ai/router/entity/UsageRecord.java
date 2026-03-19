package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账户 ID
     */
    private Long accountId;

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * Customer Token ID
     */
    private Long customerTokenId;

    /**
     * Provider ID
     */
    private Long providerId;

    /**
     * 模型 ID
     */
    private Long modelId;

    /**
     * Provider Token ID
     */
    private Long providerTokenId;

    /**
     * 请求的模型名称
     */
    private String requestModel;

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
