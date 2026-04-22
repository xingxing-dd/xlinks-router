package site.xlinks.ai.router.client.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletMainFlowResponse {
    private Long id;
    private String orderNo;
    private String bizType;
    private String direction;
    private BigDecimal changeAmount;
    private BigDecimal totalBalanceBefore;
    private BigDecimal totalBalanceAfter;
    private BigDecimal availableBalanceBefore;
    private BigDecimal availableBalanceAfter;
    private String remark;
    private LocalDateTime createdAt;
}
