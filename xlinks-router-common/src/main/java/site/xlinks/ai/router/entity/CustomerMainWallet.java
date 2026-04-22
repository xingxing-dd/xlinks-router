package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Customer main wallet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_main_wallets")
public class CustomerMainWallet extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private String walletNo;

    private BigDecimal totalBalance;

    private BigDecimal availableBalance;

    private Integer allowIn;

    private Integer allowOut;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private String remark;
}
