package site.xlinks.ai.router.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthResetPasswordRequest {

    @NotBlank(message = "重置目标不能为空")
    private String target;

    @NotBlank(message = "重置目标类型不能为空")
    private String targetType;

    @NotBlank(message = "新密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
