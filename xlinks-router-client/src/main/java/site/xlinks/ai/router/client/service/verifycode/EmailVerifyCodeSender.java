package site.xlinks.ai.router.client.service.verifycode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.client.config.MailtrapEmailProperties;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;
import site.xlinks.ai.router.client.service.MailtrapEmailClient;
import site.xlinks.ai.router.client.service.VerifyCodeService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Email verification-code sender.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerifyCodeSender implements VerifyCodeSender {

    private static final String CODE_TYPE = "email";
    private static final Map<String, String> SCENE_LABELS = Map.of(
            "register", "register",
            "resetpwd", "reset password"
    );

    private final MailtrapEmailClient mailtrapEmailClient;
    private final VerifyCodeService verifyCodeService;
    private final MailtrapEmailProperties mailtrapEmailProperties;

    @Override
    public VerifyCodeSendResponse send(String scene, String target, String token, int expireSeconds) {
        String code = verifyCodeService.getCodeByToken(token);
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Verification code is expired or missing");
        }

        String normalizedScene = normalizeScene(scene);
        String sceneLabel = resolveSceneLabel(normalizedScene);
        String subject = renderTemplate(mailtrapEmailProperties.getVerifyCodeSubjectTemplate(), normalizedScene, sceneLabel);
        String senderName = renderTemplate(mailtrapEmailProperties.getSenderNameTemplate(), normalizedScene, sceneLabel);
        String textContent = buildVerifyCodeText(code, expireSeconds, sceneLabel);
        String htmlContent = buildVerifyCodeHtml(code, expireSeconds, sceneLabel);
        mailtrapEmailClient.sendEmail(target, null, subject, textContent, htmlContent, senderName);

        VerifyCodeSendResponse response = new VerifyCodeSendResponse();
        response.setMessage("Email verification code sent successfully");
        response.setToken(token);
        response.setExpireSeconds(expireSeconds);
        return response;
    }

    @Override
    public String getSupportedCodeType() {
        return CODE_TYPE;
    }

    private String buildVerifyCodeText(String code, int expireSeconds, String sceneLabel) {
        int expireMinutes = Math.max(1, expireSeconds / 60);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String appName = defaultIfBlank(mailtrapEmailProperties.getAppName(), "xlinks");
        return "[" + appName + "] " + sceneLabel + " verification code\n"
                + "\n"
                + "Hello, you are performing the " + sceneLabel + " action in " + appName + ".\n"
                + "Your verification code is: " + code + "\n"
                + "Valid for: " + expireMinutes + " minute(s)\n"
                + "Sent at: " + now + "\n"
                + "\n"
                + "If this was not you, please ignore this email.";
    }

    private String buildVerifyCodeHtml(String code, int expireSeconds, String sceneLabel) {
        int expireMinutes = Math.max(1, expireSeconds / 60);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String appName = defaultIfBlank(mailtrapEmailProperties.getAppName(), "xlinks");
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>" + sceneLabel + " verification code</title>"
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
                + "<div class=\"header\">" + appName + " " + sceneLabel + " verification code</div>"
                + "<div class=\"content\">"
                + "<p>Hello, you are performing the " + sceneLabel + " action in " + appName + ". Please use the following verification code:</p>"
                + "<div class=\"code\">" + code + "</div>"
                + "<p>This code is valid for <strong>" + expireMinutes + " minute(s)</strong>.</p>"
                + "<div class=\"tips\">If this was not you, please ignore this email.</div>"
                + "</div>"
                + "<div class=\"footer\">Sent at: " + now + "</div>"
                + "</div>"
                + "</body></html>";
    }

    private String normalizeScene(String scene) {
        return scene == null ? "verify" : scene.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSceneLabel(String scene) {
        return SCENE_LABELS.getOrDefault(scene, "verification");
    }

    private String renderTemplate(String template, String scene, String sceneLabel) {
        String resolvedTemplate = template == null || template.isBlank()
                ? "[{appName}] {sceneLabel} verification code"
                : template;
        String appName = defaultIfBlank(mailtrapEmailProperties.getAppName(), "xlinks");
        return resolvedTemplate
                .replace("{appName}", appName)
                .replace("{scene}", scene)
                .replace("{sceneLabel}", sceneLabel);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}