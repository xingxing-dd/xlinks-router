package site.xlinks.ai.router.client.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.config.SmsProperties;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * Guoyangyun SMS client (APPCODE mode).
 *
 * Docs typically require:
 * - Authorization: APPCODE {appcode}
 * - GET {host}{path}?mobile=...&param=...&smsSignId=...&templateId=...
 *
 * This client only sends, and does not parse provider-specific JSON for now.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuoyangyunSmsClient {

    private final OkHttpClient okHttpClient;
    private final SmsProperties smsProperties;

    public void sendVerifyCode(String mobile, String code) {
        SmsProperties.Guoyangyun cfg = smsProperties.getGuoyangyun();
        if (cfg.getHost() == null || cfg.getHost().isBlank() || cfg.getPath() == null || cfg.getPath().isBlank()
                || cfg.getAppcode() == null || cfg.getAppcode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "短信通道未配置");
        }
        if (mobile == null || mobile.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }

        String url = buildUrl(cfg.getHost(), cfg.getPath(), mobile, code, cfg.getSmsSignId(), cfg.getTemplateId());
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), "");
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "APPCODE " + cfg.getAppcode())
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() == null ? "" : response.body().string();
                log.warn("Guoyangyun sms failed, httpCode={}, body={}", response.code(), body);
                throw new BusinessException(ErrorCode.PARAM_ERROR, "短信发送失败");
            }
        } catch (IOException e) {
            log.warn("Guoyangyun sms io error", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "短信发送失败");
        }
    }

    private static String buildUrl(String host, String path, String mobile, String code, String smsSignId, String templateId) {
        StringBuilder sb = new StringBuilder();
        sb.append(trimTrailingSlash(host));
        if (!path.startsWith("/")) {
            sb.append('/');
        }
        sb.append(path);

        sb.append("?mobile=").append(urlEncode(mobile));
        // Provider uses "param" field, usually like: **code:123456**
        sb.append("&param=").append(urlEncode(String.format("**code**:%s,**minute**:5", code)));

        if (smsSignId != null && !smsSignId.isBlank()) {
            sb.append("&smsSignId=").append(urlEncode(smsSignId));
        }
        if (templateId != null && !templateId.isBlank()) {
            sb.append("&templateId=").append(urlEncode(templateId));
        }
        return sb.toString();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) {
            return null;
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
