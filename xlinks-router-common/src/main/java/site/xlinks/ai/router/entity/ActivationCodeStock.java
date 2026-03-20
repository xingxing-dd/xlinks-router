package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 激活码库存实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activation_code_stocks")
public class ActivationCodeStock extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 激活码
     */
    private String activationCode;

    /**
     * 绑定套餐 ID
     */
    private Long planId;

    /**
     * 状态：1-可用，0-禁用，2-已使用
     */
    private Integer status;

    /**
     * 使用时间
     */
    private LocalDateTime usedAt;

    /**
     * 使用者账号 ID
     */
    private Long usedBy;

    /**
     * 对应客户订阅记录 ID
     */
    private Long subscriptionId;

    /**
     * 支付订单号
     */
    private String orderId;

    /**
     * 备注
     */
    private String remark;
}
