package site.xlinks.ai.router.client.dto.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserInfoResponse {
    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private BigDecimal balance;
    private Integer status;
    private String createdAt;
}
