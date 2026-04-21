package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer subscription snapshot entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_plans")
public class CustomerPlan extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private Long planId;

    private String planName;

    private BigDecimal price;

    private Integer durationDays;

    private BigDecimal dailyQuota;

    private BigDecimal totalQuota;

    /**
     * Cache-hit billing multiplier snapshot copied from the source plan.
     */
    private BigDecimal multiplier;

    private BigDecimal usedQuota;

    private BigDecimal totalUsedQuota;

    private LocalDateTime quotaRefreshTime;

    private LocalDateTime planExpireTime;

    private Integer status;

    private String source;

    private String remark;
}
