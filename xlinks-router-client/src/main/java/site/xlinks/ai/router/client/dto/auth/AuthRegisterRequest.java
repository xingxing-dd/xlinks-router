package site.xlinks.ai.router.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRegisterRequest {

    @NotBlank(message = "账号不能为空")
    private String target;

    @NotBlank(message = "账号类型不能为空")
    private String targetType;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String code;

    private String inviteCode;
}
