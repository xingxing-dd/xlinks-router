package site.xlinks.ai.router.client.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageItemResponse {
    private String model;
    private Integer requests;
    private Integer tokens;
    private BigDecimal cost;
}
