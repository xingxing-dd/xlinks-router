package site.xlinks.ai.router.client.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOptionResponse {
    private BigDecimal usd;
    private BigDecimal cny;
    private BigDecimal bonus;
}
