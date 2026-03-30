package site.xlinks.ai.router.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.config.BrevoEmailProperties;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Brevo邮件客户端
 * 使用Brevo SMTP API发送邮件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailClient {

    private final OkHttpClient okHttpClient;
    private final BrevoEmailProperties brevoProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送邮件
     *
     * @param toEmail     收件人邮箱
     * @param toName      收件人名称
     * @param subject    邮件主题
     * @param htmlContent HTML邮件内容
     * @return 发送成功返回messageId
     */
    public String sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (!brevoProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮件服务未启用");
        }
        if (brevoProperties.getAccessToken() == null || brevoProperties.getAccessToken().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮件API密钥未配置");
        }
        if (toEmail == null || toEmail.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收件人邮箱不能为空");
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = buildEmailRequest(toEmail, toName, subject, htmlContent);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonBody
            );

            Request request = new Request.Builder()
                    .url(brevoProperties.getApiUrl())
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("api-key", brevoProperties.getAccessToken())
                    .addHeader("content-type", "application/json")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() == null ? "" : response.body().string();
                    log.error("Brevo email send failed, httpCode={}, body={}", response.code(), responseBody);
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "邮件发送失败");
                }

                String responseBody = response.body() == null ? "" : response.body().string();
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                
                @SuppressWarnings("unchecked")
                List<String> messageIds = (List<String>) result.get("messageIds");
                if (messageIds != null && !messageIds.isEmpty()) {
                    log.info("Brevo email sent successfully, messageId={}, to={}", messageIds.get(0), toEmail);
                    return messageIds.get(0);
                } else {
                    log.error("Brevo email send failed, no messageId returned, body={}", responseBody);
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "邮件发送失败");
                }
            }
        } catch (IOException e) {
            log.error("Brevo email send io error", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮件发送失败");
        }
    }

    private Map<String, Object> buildEmailRequest(String toEmail, String toName, String subject, String htmlContent) {
        return Map.of(
                "sender", Map.of(
                        "email", brevoProperties.getSenderEmail() != null ? brevoProperties.getSenderEmail() : "xingxingdd132311@gmail.com",
                        "name", brevoProperties.getSenderName() != null ? brevoProperties.getSenderName() : "xlinks"
                ),
                "subject", subject,
                "htmlContent", htmlContent,
                "messageVersions", Collections.singletonList(
                        Map.of(
                                "to", Collections.singletonList(
                                        Map.of(
                                                "email", toEmail,
                                                "name", toName != null ? toName : toEmail
                                        )
                                )
                        )
                )
        );
    }
}