package site.xlinks.ai.router.client.dto.promotion;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionRuleResponse {
    private BigDecimal registerReward;
    private BigDecimal firstRechargeRate;
    private BigDecimal consumptionRate;
    private Integer settlementDay;
}
