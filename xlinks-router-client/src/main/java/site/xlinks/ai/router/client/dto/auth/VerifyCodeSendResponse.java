package site.xlinks.ai.router.client.dto.auth;

import lombok.Data;

@Data
public class VerifyCodeSendResponse {

    private String message;

    private String mockCode;

    private Integer expireSeconds;
}
