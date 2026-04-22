package site.xlinks.ai.router.client.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletSubAccountResponse {
    private String walletNo;
    private String walletType;
    private BigDecimal balance;
    private Integer status;
    private String remark;
}
