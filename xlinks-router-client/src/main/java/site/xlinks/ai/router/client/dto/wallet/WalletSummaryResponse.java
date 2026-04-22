package site.xlinks.ai.router.client.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WalletSummaryResponse {
    private String walletNo;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private Integer allowIn;
    private Integer allowOut;
    private Integer status;
    private List<WalletSubAccountResponse> subWallets;
}
