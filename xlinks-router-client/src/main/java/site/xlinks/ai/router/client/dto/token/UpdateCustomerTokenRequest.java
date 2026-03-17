package site.xlinks.ai.router.client.dto.token;

import lombok.Data;

import java.util.List;

@Data
public class UpdateCustomerTokenRequest {
    private String tokenName;
    private List<String> allowedModels;
    private Integer status;
    private String expireTime;
}
