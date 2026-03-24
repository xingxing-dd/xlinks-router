package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 推广规则配置实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_rules")
public class PromotionRule extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;

    private String ruleName;

    private Integer rewardType;

    private BigDecimal rewardAmount;

    private BigDecimal rewardRate;

    private Integer settlementDay;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private String iconType;
}