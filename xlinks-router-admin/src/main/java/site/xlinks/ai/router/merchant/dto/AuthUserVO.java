package site.xlinks.ai.router.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录用户信息。
 */
@Data
@Builder
public class AuthUserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态")
    private Integer status;
}