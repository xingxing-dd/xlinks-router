package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户订阅实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_plans")
public class CustomerPlan extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 客户账户 ID
     */
    private Long accountId;

    /**
     * 订阅套餐 ID
     */
    private Long planId;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 订阅价格（元）
     */
    private BigDecimal price;

    /**
     * 订阅周期天数
     */
    private Integer durationDays;

    /**
     * 每日额度（美元）
     */
    private BigDecimal dailyQuota;

    /**
     * 总额度（美元）
     */
    private BigDecimal totalQuota;

    /**
     * 已使用额度（美元）
     */
    private BigDecimal usedQuota;

    /**
     * 总共已使用额度（美元）
     */
    private BigDecimal totalUsedQuota;

    /**
     * 额度刷新时间
     */
    private LocalDateTime quotaRefreshTime;

    /**
     * 订阅过期时间
     */
    private LocalDateTime planExpireTime;

    /**
     * 状态：1-生效，0-失效
     */
    private Integer status;

    /**
     * 订阅来源：purchase/grant/admin 等
     */
    private String source;

    /**
     * 备注
     */
    private String remark;
}
