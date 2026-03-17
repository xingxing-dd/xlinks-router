package site.xlinks.ai.router.client.dto.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTokenItemResponse {
    private Long id;
    private String customerName;
    private String tokenName;
    private String tokenValue;
    private Integer status;
    private String expireTime;
    private List<String> allowedModels;
    private Integer totalRequests;
    private String lastUsedAt;
    private String createdAt;
}
