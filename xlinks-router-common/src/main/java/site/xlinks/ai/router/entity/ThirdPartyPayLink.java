package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 第三方支付链接实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("third_party_pay_links")
public class ThirdPartyPayLink extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 目标ID（如 plan_id）
     */
    private Long targetId;

    /**
     * 目标类型（如 plan）
     */
    private String targetType;

    /**
     * 第三方支付跳转链接
     */
    private String payUrl;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
