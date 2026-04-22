package site.xlinks.ai.router.client.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletSubFlowResponse {
    private Long id;
    private String orderNo;
    private String walletType;
    private String bizType;
    private String direction;
    private BigDecimal changeAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createdAt;
}
