package site.xlinks.ai.router.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应。
 */
@Data
@Builder
public class AuthTokenVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "过期秒数")
    private Long expiresIn;

    @Schema(description = "登录用户信息")
    private AuthUserVO user;
}