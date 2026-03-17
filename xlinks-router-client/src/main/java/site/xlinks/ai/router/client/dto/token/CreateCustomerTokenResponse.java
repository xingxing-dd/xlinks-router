package site.xlinks.ai.router.client.dto.token;

import lombok.Data;

@Data
public class CreateCustomerTokenResponse {
    private Long id;
    private String tokenName;
    private String tokenValue;
    private String expireTime;
    private String createdAt;
}
