package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified payment order entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_orders")
public class CustomerOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Platform order id.
     */
    private String orderNo;

    /**
     * Payment channel order reference, e.g. trade_no.
     */
    private String refNo;

    private Long accountId;

    /**
     * recharge / withdraw / subscription_purchase ...
     */
    private String orderType;

    private String orderTitle;

    /**
     * JSON snapshot for business context.
     */
    private String orderInfo;

    /**
     * alipay / wechat / third-party ...
     */
    private String paymentChannel;

    private BigDecimal totalAmount;

    /**
     * 0 pending, 1 paid, 2 failed, 3 closed, 4 refunded.
     */
    private Integer status;

    private LocalDateTime completeAt;

    /**
     * Payment expires at, unpaid orders should be closed after this time.
     */
    private LocalDateTime expiredAt;

    private String remark;
}

