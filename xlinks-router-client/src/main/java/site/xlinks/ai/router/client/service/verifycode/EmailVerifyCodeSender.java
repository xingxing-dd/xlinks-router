package site.xlinks.ai.router.client.service.verifycode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;

/**
 * 邮箱验证码发送策略
 * 预留扩展，目前返回模拟成功消息，后续可接入真实的邮件服务
 */
@Slf4j
@Component
public class EmailVerifyCodeSender implements VerifyCodeSender {

    private static final String CODE_TYPE = "email";

    @Override
    public VerifyCodeSendResponse send(String target, String token, int expireSeconds) {
        // TODO: 实现真实的邮箱发送逻辑
        // 可以接入 Spring Mail 或其他邮件服务
        log.info("Email verify code token: {}, expire: {}s (mock implementation)", token, expireSeconds);

        // Keep behavior consistent with previous mock: still return success message,
        // but do not expose real code in production.
        VerifyCodeSendResponse response = new VerifyCodeSendResponse();
        response.setMessage("邮箱验证码发送成功");
        response.setToken(token);
        response.setExpireSeconds(expireSeconds);
        return response;
    }

    @Override
    public String getSupportedCodeType() {
        return CODE_TYPE;
    }
}