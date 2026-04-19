package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Notifies API module to refresh local caches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCacheRefreshNotifier {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Value("${xlinks.internal.cache-refresh.enabled:false}")
    private boolean enabled;

    @Value("${xlinks.internal.cache-refresh.api-url:}")
    private String apiUrl;

    @Value("${xlinks.internal.cache-refresh.token:}")
    private String token;

    public void notifyAdminCacheChanged(String type, String action, Long id) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "admin");
        payload.put("type", type);
        payload.put("action", action == null ? "updated" : action);
        payload.put("remark", "admin configuration changed");
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            log.warn("Cache refresh notify failed. payload={}", payload, throwable);
                            return;
                        }
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            log.warn("Cache refresh notify returned non-success status={} payload={}",
                                    response.statusCode(), payload);
                            return;
                        }
                        log.debug("Cache refresh notify succeeded. payload={}", payload);
                    });
        } catch (Exception ex) {
            log.warn("Failed to submit cache refresh notify request. payload={}", payload, ex);
        }
    }
}
