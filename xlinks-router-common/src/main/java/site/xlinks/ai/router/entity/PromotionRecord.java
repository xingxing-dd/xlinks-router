package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推广记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_records")
public class PromotionRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long inviterUserId;

    private Long inviteeUserId;

    private String inviteCode;

    private Integer rewardType;

    private BigDecimal rewardAmount;

    private BigDecimal rewardRate;

    private Integer status;

    private LocalDateTime settleAt;

    private String sourceOrderNo;

    private String remark;
}