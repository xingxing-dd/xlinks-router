package site.xlinks.ai.router.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletSubWalletVO {
    private String walletNo;
    private String walletType;
    private BigDecimal balance;
    private Integer status;
    private String remark;
}
