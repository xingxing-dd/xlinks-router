package site.xlinks.ai.router.client.service.verifycode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;
import site.xlinks.ai.router.client.service.BrevoEmailClient;
import site.xlinks.ai.router.client.service.VerifyCodeService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮箱验证码发送策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerifyCodeSender implements VerifyCodeSender {

    private static final String CODE_TYPE = "email";
    private static final String SUBJECT = "【xlinks】注册验证码";

    private final BrevoEmailClient brevoEmailClient;
    private final VerifyCodeService verifyCodeService;

    @Override
    public VerifyCodeSendResponse send(String target, String token, int expireSeconds) {
        // 通过 token 从 Redis 获取验证码
        String code = verifyCodeService.getCodeByToken(token);
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("验证码已过期或不存在");
        }

        String htmlContent = buildVerifyCodeHtml(code, expireSeconds);
        brevoEmailClient.sendEmail(target, null, SUBJECT, htmlContent);

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

    private String buildVerifyCodeHtml(String code, int expireSeconds) {
        int expireMinutes = Math.max(1, expireSeconds / 60);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "<!DOCTYPE html>"
                + "<html lang=\"zh-CN\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>注册验证码</title>"
                + "<style>body{font-family:Arial,Helvetica,sans-serif;background:#f6f8fb;color:#333;margin:0;padding:0;}"
                + ".container{max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.06);}"
                + ".header{background:#1e88e5;color:#fff;padding:24px;text-align:center;font-size:20px;font-weight:600;}"
                + ".content{padding:24px;font-size:15px;line-height:1.6;}"
                + ".code{font-size:28px;font-weight:700;letter-spacing:6px;margin:16px 0;color:#1e88e5;}"
                + ".tips{background:#f1f4f9;padding:12px;border-radius:6px;color:#555;margin-top:16px;}"
                + ".footer{padding:16px 24px;font-size:12px;color:#999;text-align:center;border-top:1px solid #eee;}"
                + "</style></head>"
                + "<body>"
                + "<div class=\"container\">"
                + "<div class=\"header\">xlinks 注册验证码</div>"
                + "<div class=\"content\">"
                + "<p>您好，您正在进行 xlinks 注册操作，请使用以下验证码完成验证：</p>"
                + "<div class=\"code\">" + code + "</div>"
                + "<p>验证码有效期为 <strong>" + expireMinutes + " 分钟</strong>，请尽快完成验证。</p>"
                + "<div class=\"tips\">如果这不是您的操作，请忽略此邮件。</div>"
                + "</div>"
                + "<div class=\"footer\">发送时间：" + now + "</div>"
                + "</div>"
                + "</body></html>";
    }
}