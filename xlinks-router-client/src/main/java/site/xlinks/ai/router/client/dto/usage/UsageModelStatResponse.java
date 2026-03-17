package site.xlinks.ai.router.client.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageModelStatResponse {
    private String model;
    private Integer requests;
    private Integer tokens;
    private BigDecimal cost;
}
