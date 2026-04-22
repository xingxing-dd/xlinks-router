package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Sub wallet flow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_sub_wallet_flows")
public class CustomerSubWalletFlow extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long subWalletId;

    private Long mainWalletId;

    private Long accountId;

    private String orderNo;

    private String walletType;

    private String bizType;

    private String direction;

    private BigDecimal changeAmount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String remark;
}
