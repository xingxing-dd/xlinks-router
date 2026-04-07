package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 第三方支付订单实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("third_party_pay_orders")
public class ThirdPartyPayOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private String thirdPartyOrderNo;

    private Long accountId;

    private Long targetId;

    private String targetType;

    private String paymentMethodCode;

    private String paymentMethodType;

    private String orderTitle;

    private BigDecimal totalAmount;

    private Integer status;

    private String tradeStatus;

    private String payUrl;

    private LocalDateTime payTime;

    private LocalDateTime expiredAt;

    private String remark;
}