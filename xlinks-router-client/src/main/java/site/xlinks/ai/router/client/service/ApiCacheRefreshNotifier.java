package site.xlinks.ai.router.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Notifies API module to refresh local caches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCacheRefreshNotifier {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${xlinks.internal.cache-refresh.enabled:false}")
    private boolean enabled;

    @Value("${xlinks.internal.cache-refresh.api-url:}")
    private String apiUrl;

    @Value("${xlinks.internal.cache-refresh.token:}")
    private String token;

    public void notifyCustomerTokenChanged(Long accountId, Long id, String action) {
        if (accountId == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "client");
        payload.put("type", "customerToken");
        payload.put("action", action == null ? "updated" : action);
        payload.put("accountId", accountId);
        payload.put("remark", "customer token changed from client module");
        if (id != null) {
            payload.put("id", id);
        }
        sendAsync(payload);
    }

    private void sendAsync(Map<String, Object> payload) {
        if (!enabled) {
            log.debug("Skip cache refresh notify because feature is disabled. payload={}", payload);
            return;
        }
        if (apiUrl == null || apiUrl.isBlank() || token == null || token.isBlank()) {
            log.warn("Skip cache refresh notify because apiUrl or token is blank. payload={}", payload);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(RequestBody.create(json, JSON))
                    .build();
            okHttpClient.newCall(request).enqueue(new LoggingCallback(payload));
        } catch (Exception ex) {
            log.warn("Failed to submit cache refresh notify request. payload={}", payload, ex);
        }
    }

    private static class LoggingCallback implements Callback {
        private final Map<String, Object> payload;

        private LoggingCallback(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            log.warn("Cache refresh notify failed. payload={}", payload, e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try (response) {
                if (!response.isSuccessful()) {
                    log.warn("Cache refresh notify returned non-success status={} payload={}",
                            response.code(), payload);
                    return;
                }
                log.debug("Cache refresh notify succeeded. payload={}", payload);
            }
        }
    }
}
