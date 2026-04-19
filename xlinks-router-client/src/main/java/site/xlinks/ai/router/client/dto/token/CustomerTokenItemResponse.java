package site.xlinks.ai.router.client.dto.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTokenItemResponse {
    private String id;
    private String customerName;
    private String tokenName;
    private String tokenValue;
    private Integer status;
    private String expireTime;
    private List<String> allowedModels;
    private BigDecimal dailyQuota;
    private BigDecimal usedQuota;
    private BigDecimal totalQuota;
    private BigDecimal totalUsedQuota;
    private Integer totalRequests;
    private String lastUsedAt;
    private String createdAt;
}
