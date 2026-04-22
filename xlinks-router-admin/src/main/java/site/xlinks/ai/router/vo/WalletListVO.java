package site.xlinks.ai.router.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletListVO {
    private Long accountId;
    private String username;
    private String phone;
    private String email;
    private String walletNo;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private Integer allowIn;
    private Integer allowOut;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
