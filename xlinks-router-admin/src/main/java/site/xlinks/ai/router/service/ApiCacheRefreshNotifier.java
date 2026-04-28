package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        dispatch(payload);
    }

    private void dispatch(Map<String, Object> payload) {
        Map<String, Object> immutablePayload = Map.copyOf(payload);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(immutablePayload);
                }
            });
            log.debug("Cache refresh notify deferred until transaction commit. payload={}", immutablePayload);
            return;
        }
        send(immutablePayload);
    }

    private void send(Map<String, Object> payload) {
        if (!enabled) {
            log.debug("Skip cache refresh notify because feature is disabled. payload={}", payload);
            return;
        }
        List<String> apiUrls = resolveApiUrls();
        if (apiUrls.isEmpty() || token == null || token.isBlank()) {
            log.warn("Skip cache refresh notify because apiUrl or token is blank. payload={}", payload);
            return;
        }
        for (String targetUrl : apiUrls) {
            sendToTarget(targetUrl, payload);
        }
    }

    private List<String> resolveApiUrls() {
        if (apiUrl == null || apiUrl.isBlank()) {
            return List.of();
        }
        return Arrays.stream(apiUrl.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .distinct()
                .toList();
    }

    private void sendToTarget(String targetUrl, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Cache refresh notify returned non-success status={} targetUrl={} payload={} body={}",
                        response.statusCode(), targetUrl, payload, response.body());
                return;
            }
            log.info("Cache refresh notify succeeded. targetUrl={} payload={}", targetUrl, payload);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Cache refresh notify interrupted. targetUrl={} payload={}", targetUrl, payload, ex);
        } catch (Exception ex) {
            log.warn("Failed to send cache refresh notify request. targetUrl={} payload={}", targetUrl, payload, ex);
        }
    }
}
