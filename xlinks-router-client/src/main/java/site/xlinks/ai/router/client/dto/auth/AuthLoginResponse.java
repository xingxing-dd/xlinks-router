package site.xlinks.ai.router.client.dto.auth;

import lombok.Data;

@Data
public class AuthLoginResponse {

    private String accessToken;

    private Long expiresIn;

    private AuthUser user;

    @Data
    public static class AuthUser {
        private Long id;
        private String email;
        private Integer status;
    }
}
