package site.xlinks.ai.router.client.dto.token;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCustomerTokenResponse {
    private String id;
    private String tokenName;
    private String tokenValue;
    private String expireTime;
    private BigDecimal dailyQuota;
    private BigDecimal totalQuota;
    private String createdAt;
}
