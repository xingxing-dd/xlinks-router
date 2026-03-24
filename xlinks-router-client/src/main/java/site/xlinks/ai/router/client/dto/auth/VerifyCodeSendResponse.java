package site.xlinks.ai.router.client.dto.auth;

import lombok.Data;

@Data
public class VerifyCodeSendResponse {

    private String message;

    /**
     * 验证码发送 token，用于后续验证时从 Redis 中取出验证码
     */
    private String token;

    private Integer expireSeconds;
}
