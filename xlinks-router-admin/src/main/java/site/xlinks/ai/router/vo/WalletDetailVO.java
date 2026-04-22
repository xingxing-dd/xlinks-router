package site.xlinks.ai.router.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WalletDetailVO {
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
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WalletSubWalletVO> subWallets;
}
