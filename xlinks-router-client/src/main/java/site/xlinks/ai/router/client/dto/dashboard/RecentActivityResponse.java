package site.xlinks.ai.router.client.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String time;
    private String token;
    private String channel;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private java.math.BigDecimal cost;
}
