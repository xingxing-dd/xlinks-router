package site.xlinks.ai.router.client.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeSendRequest {

    @NotBlank(message = "验证码类型不能为空")
    private String codeType;

    private String target;

    @NotBlank(message = "场景不能为空")
    private String scene;
}
