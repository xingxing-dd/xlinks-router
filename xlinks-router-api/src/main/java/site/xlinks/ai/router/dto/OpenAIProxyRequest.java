package site.xlinks.ai.router.dto;

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

    private boolean stream;

    private String requestBody;
}
