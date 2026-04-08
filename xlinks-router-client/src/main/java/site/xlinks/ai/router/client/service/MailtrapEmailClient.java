package site.xlinks.ai.router.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.config.MailtrapEmailProperties;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mailtrap mail client.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailtrapEmailClient {

    private final OkHttpClient okHttpClient;
    private final MailtrapEmailProperties mailtrapProperties;
    private final ObjectMapper objectMapper;

    /**
     * Send email.
     */
    public String sendEmail(String toEmail,
                            String toName,
                            String subject,
                            String textContent,
                            String htmlContent) {
        return sendEmail(toEmail, toName, subject, textContent, htmlContent, null);
    }

    /**
     * Send email with optional sender name override.
     */
    public String sendEmail(String toEmail,
                            String toName,
                            String subject,
                            String textContent,
                            String htmlContent,
                            String fromName) {
        if (!mailtrapProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Mail service is disabled");
        }
        if (mailtrapProperties.getAccessToken() == null || mailtrapProperties.getAccessToken().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Mail API access token is not configured");
        }
        if (toEmail == null || toEmail.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Recipient email must not be blank");
        }
        if ((textContent == null || textContent.isBlank()) && (htmlContent == null || htmlContent.isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Mail content must not be blank");
        }

        try {
            Map<String, Object> requestBody = buildEmailRequest(toEmail, toName, subject, textContent, htmlContent, fromName);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            Request request = new Request.Builder()
                    .url(mailtrapProperties.getApiUrl())
                    .post(body)
                    .addHeader("Authorization", "Bearer " + mailtrapProperties.getAccessToken())
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    log.error("Mailtrap email send failed, httpCode={}, body={}", response.code(), responseBody);
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "Failed to send email");
                }

                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                boolean success = Boolean.TRUE.equals(result.get("success"));

                @SuppressWarnings("unchecked")
                List<Object> messageIds = (List<Object>) result.get("message_ids");
                if (success && messageIds != null && !messageIds.isEmpty()) {
                    String messageId = String.valueOf(messageIds.get(0));
                    log.info("Mailtrap email sent successfully, messageId={}, to={}", messageId, toEmail);
                    return messageId;
                }

                log.error("Mailtrap email send failed, invalid response body={}", responseBody);
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Failed to send email");
            }
        } catch (IOException e) {
            log.error("Mailtrap email send io error", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Failed to send email");
        }
    }

    private Map<String, Object> buildEmailRequest(String toEmail,
                                                  String toName,
                                                  String subject,
                                                  String textContent,
                                                  String htmlContent,
                                                  String fromName) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("from", Map.of(
                "email", defaultIfBlank(mailtrapProperties.getSenderEmail(), "tech@xlinks.site"),
                "name", defaultIfBlank(fromName, defaultIfBlank(mailtrapProperties.getSenderName(), mailtrapProperties.getAppName()))
        ));

        List<Map<String, Object>> recipients = new ArrayList<>();
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("email", toEmail);
        if (toName != null && !toName.isBlank()) {
            recipient.put("name", toName);
        }
        recipients.add(recipient);

        request.put("to", recipients);
        request.put("subject", subject);
        if (textContent != null && !textContent.isBlank()) {
            request.put("text", textContent);
        }
        if (htmlContent != null && !htmlContent.isBlank()) {
            request.put("html", htmlContent);
        }
        if (mailtrapProperties.getCategory() != null && !mailtrapProperties.getCategory().isBlank()) {
            request.put("category", mailtrapProperties.getCategory());
        }
        return request;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}