package site.xlinks.ai.router.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户登录请求。
 */
@Data
public class UserLoginDTO {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "RSA 加密后的密码")
    private String encryptedPassword;

    @Schema(description = "短信验证码")
    @Pattern(regexp = "^$|^\\d{6}$", message = "短信验证码必须为 6 位数字")
    private String smsCode;

    @Schema(description = "手机号，短信登录时必填")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;
}