package site.xlinks.ai.router.client.service.verifycode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;
import site.xlinks.ai.router.client.service.GuoyangyunSmsClient;
import site.xlinks.ai.router.client.service.VerifyCodeService;

/**
 * SMS verification-code sender.
 */
@Component
@RequiredArgsConstructor
public class SmsVerifyCodeSender implements VerifyCodeSender {

    private static final String CODE_TYPE = "phone";

    private final GuoyangyunSmsClient smsClient;
    private final VerifyCodeService verifyCodeService;

    @Override
    public VerifyCodeSendResponse send(String scene, String target, String token, int expireSeconds) {
        String code = verifyCodeService.getCodeByToken(token);
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Verification code is expired or missing");
        }

        smsClient.sendVerifyCode(target, code);

        VerifyCodeSendResponse response = new VerifyCodeSendResponse();
        response.setMessage("SMS verification code sent successfully");
        response.setToken(token);
        response.setExpireSeconds(expireSeconds);
        return response;
    }

    @Override
    public String getSupportedCodeType() {
        return CODE_TYPE;
    }
}