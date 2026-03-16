package site.xlinks.ai.router.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册请求。
 */
@Data
public class UserRegisterDTO {

    @Schema(description = "RSA 加密后的密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "加密密码不能为空")
    private String encryptedPassword;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;
}