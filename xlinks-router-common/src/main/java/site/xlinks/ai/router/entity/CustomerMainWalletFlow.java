package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Main wallet flow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_main_wallet_flows")
public class CustomerMainWalletFlow extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long mainWalletId;

    private Long accountId;

    private String orderNo;

    private String bizType;

    private String direction;

    private BigDecimal changeAmount;

    private BigDecimal totalBalanceBefore;

    private BigDecimal totalBalanceAfter;

    private BigDecimal availableBalanceBefore;

    private BigDecimal availableBalanceAfter;

    private String remark;
}
