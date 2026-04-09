package site.xlinks.ai.router.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

/**
 * Minimal request envelope used by the proxy pipeline.
 */
@Data
@Builder
public class OpenAIProxyRequest {

    private OpenAIProtocol protocol;

    private String model;

    private Boolean stream;

    private JsonNode payload;

    private String requestBody;

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

    public boolean hasExplicitStreamFlag() {
        return stream != null;
    }
}
