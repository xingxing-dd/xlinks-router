package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Usage record entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("usage_records")
public class UsageRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private String customerToken;

    private String providerToken;

    private Long providerTokenId;

    /**
     * balance / plan
     */
    private String usageType;

    /**
     * plan id when usageType = plan
     */
    private String usageFrom;

    private String requestId;

    private Long providerId;

    private String providerCode;

    private String providerName;

    private String endpointCode;

    private Long modelId;

    private String modelCode;

    private String modelName;

    private Integer responseStatus;

    /**
     * Input tokens reported by provider.
     */
    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    /**
     * Cache-hit tokens included in promptTokens.
     */
    private Integer cacheHitTokens;

    /**
     * Cost of non-cache-hit prompt tokens.
     */
    private BigDecimal promptCost;

    private BigDecimal cacheHitCost;

    private BigDecimal completionCost;

    private BigDecimal totalCost;

    /**
     * Time to first response data in milliseconds.
     * Non-streaming requests should use the same value as sessionMs.
     */
    private Integer responseMs;

    private Integer sessionMs;

    private String errorCode;

    private String errorMessage;

    private String finishReason;

}
