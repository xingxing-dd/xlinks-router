package site.xlinks.ai.router.client.dto.plan;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanItemResponse {
    private String id;
    private String name;
    private BigDecimal price;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyQuota;
    private List<String> features;
    private Boolean isRecommended;
    private Integer durationDays;

    public void setRecommended(Boolean recommended) {
        isRecommended = recommended;
    }
}
