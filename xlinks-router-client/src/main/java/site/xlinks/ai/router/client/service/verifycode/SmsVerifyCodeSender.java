package site.xlinks.ai.router.client.service.verifycode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;
import site.xlinks.ai.router.client.service.GuoyangyunSmsClient;
import site.xlinks.ai.router.client.service.VerifyCodeService;

/**
 * 短信验证码发送策略
 */
@Component
@RequiredArgsConstructor
public class SmsVerifyCodeSender implements VerifyCodeSender {

    private static final String CODE_TYPE = "sms";

    private final GuoyangyunSmsClient smsClient;
    private final VerifyCodeService verifyCodeService;

    @Override
    public VerifyCodeSendResponse send(String target, String token, int expireSeconds) {
        // 通过 token 从 Redis 获取验证码
        String code = verifyCodeService.getCodeByToken(token);
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("验证码已过期或不存在");
        }

        // 调用短信客户端发送验证码
        smsClient.sendVerifyCode(target, code);

        // 构建响应
        VerifyCodeSendResponse response = new VerifyCodeSendResponse();
        response.setMessage("短信验证码发送成功");
        response.setToken(token);
        response.setExpireSeconds(expireSeconds);
        return response;
    }

    @Override
    public String getSupportedCodeType() {
        return CODE_TYPE;
    }
}