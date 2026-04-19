package site.xlinks.ai.router.client.dto.token;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateCustomerTokenRequest {
    private String tokenName;
    private List<String> allowedModels;
    private Integer status;
    private String expireTime;
    private BigDecimal dailyQuota;
    private BigDecimal totalQuota;
}
