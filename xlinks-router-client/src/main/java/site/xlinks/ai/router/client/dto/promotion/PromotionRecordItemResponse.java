package site.xlinks.ai.router.client.dto.promotion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRecordItemResponse {
    private String id;
    private String userName;
    private String email;
    private String joinDate;
    private String status;
    private BigDecimal earnings;
}
