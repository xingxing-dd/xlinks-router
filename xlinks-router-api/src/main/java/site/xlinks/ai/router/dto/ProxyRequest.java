package site.xlinks.ai.router.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.Locale;
import java.util.Map;

/**
 * Minimal request envelope used by the proxy pipeline.
 */
@Data
@Builder
public class ProxyRequest {

    private ProxyProtocol protocol;

    private String model;

    private Boolean stream;

    private JsonNode payload;

    private String requestBody;

    private Map<String, String> passthroughHeaders;

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

    public boolean hasExplicitStreamFlag() {
        return stream != null;
    }

    public String getPassthroughHeader(String headerName) {
        if (passthroughHeaders == null || passthroughHeaders.isEmpty() || headerName == null || headerName.isBlank()) {
            return null;
        }
        String exact = passthroughHeaders.get(headerName);
        if (exact != null && !exact.isBlank()) {
            return exact;
        }
        String lowerCase = passthroughHeaders.get(headerName.toLowerCase(Locale.ROOT));
        if (lowerCase != null && !lowerCase.isBlank()) {
            return lowerCase;
        }
        return null;
    }
}

