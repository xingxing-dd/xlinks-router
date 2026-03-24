package site.xlinks.ai.router.client.dto.promotion;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionRuleResponse {
    private BigDecimal registerReward;
    private BigDecimal firstRechargeRate;
    private BigDecimal consumptionRate;
    private Integer settlementDay;
    private String settlementDescription;
    private List<RuleItem> rules;

    @Data
    public static class RuleItem {
        private String ruleCode;
        private String ruleName;
        private Integer rewardType;
        private BigDecimal rewardAmount;
        private BigDecimal rewardRate;
        private Integer settlementDay;
        private String description;
        private Integer sortOrder;
        private String iconType;
    }
}
