package site.xlinks.ai.router.client.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardStatsResponse {
    private Long todayRequests;
    private Double todayRequestsChange;
    private Long todayTokens;
    private Double todayTokensChange;
    private BigDecimal todayCost;
    private Double todayCostChange;
    private BigDecimal balance;
}
