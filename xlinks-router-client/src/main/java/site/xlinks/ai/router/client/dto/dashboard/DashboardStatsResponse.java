package site.xlinks.ai.router.client.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardStatsResponse {
    private Integer todayRequests;
    private Double todayRequestsChange;
    private Integer todayTokens;
    private Double todayTokensChange;
    private BigDecimal todayCost;
    private Double todayCostChange;
    private BigDecimal balance;
}
