package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Customer sub wallet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_sub_wallets")
public class CustomerSubWallet extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long mainWalletId;

    private String walletNo;

    private String walletType;

    private BigDecimal balance;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private String remark;
}
