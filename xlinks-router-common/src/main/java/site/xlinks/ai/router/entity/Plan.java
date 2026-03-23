package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订阅套餐实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plans")
public class Plan extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 套餐价格（元）
     */
    private BigDecimal price;

    /**
     * 有效期天数
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
     * 允许访问的模型列表（JSON）
     */
    private String allowedModels;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 是否可见：1-可见，0-隐藏
     */
    private Integer visible;

    /**
     * 备注
     */
    private String remark;
}
