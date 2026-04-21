package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Subscription plan entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plans")
public class Plan extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String planName;

    private BigDecimal price;

    private Integer durationDays;

    private BigDecimal dailyQuota;

    private BigDecimal totalQuota;

    /**
     * Multiplier used for cache-hit billing adjustments.
     */
    private BigDecimal multiplier;

    /**
     * Max purchase count per account. Null means unlimited.
     */
    private Integer maxPurchaseCount;

    private String allowedModels;

    private Integer status;

    private Integer visible;

    private String remark;
}
