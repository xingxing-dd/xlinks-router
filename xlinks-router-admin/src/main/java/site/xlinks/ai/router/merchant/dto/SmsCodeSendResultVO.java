package site.xlinks.ai.router.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 发送短信验证码响应。
 */
@Data
@Builder
public class SmsCodeSendResultVO {

    @Schema(description = "发送结果描述")
    private String message;

    @Schema(description = "开发/测试环境返回的验证码")
    private String mockCode;

    @Schema(description = "验证码有效期，秒")
    private Long expireSeconds;
}