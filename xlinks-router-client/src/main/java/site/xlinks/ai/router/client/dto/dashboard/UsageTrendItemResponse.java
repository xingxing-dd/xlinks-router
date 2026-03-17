package site.xlinks.ai.router.client.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageTrendItemResponse {
    private String date;
    private Integer tokens;
    private BigDecimal cost;
}
