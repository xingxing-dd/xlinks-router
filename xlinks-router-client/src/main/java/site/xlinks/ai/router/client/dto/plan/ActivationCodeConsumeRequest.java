package site.xlinks.ai.router.client.dto.plan;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 激活码兑换请求
 */
@Data
public class ActivationCodeConsumeRequest {

    @NotBlank(message = "激活码不能为空")
    private String code;
}
