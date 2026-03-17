package site.xlinks.ai.router.client.dto.promotion;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionInfoResponse {
    private String referralCode;
    private String referralLink;
    private Integer totalReferrals;
    private Integer activeReferrals;
    private BigDecimal totalEarnings;
    private BigDecimal pendingEarnings;
}
