package site.xlinks.ai.router.protocol.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnthropicProtocolErrorResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String type;
    private Error error;

    public static AnthropicProtocolErrorResponse invalidRequest(String message) {
        return new AnthropicProtocolErrorResponse("error", new Error("invalid_request_error", message));
    }

    public static AnthropicProtocolErrorResponse authenticationError(String message) {
        return new AnthropicProtocolErrorResponse("error", new Error("authentication_error", message));
    }

    public static AnthropicProtocolErrorResponse rateLimitError(String message) {
        return new AnthropicProtocolErrorResponse("error", new Error("rate_limit_error", message));
    }

    public static AnthropicProtocolErrorResponse apiError(String message) {
        return new AnthropicProtocolErrorResponse("error", new Error("api_error", message));
    }

    public static AnthropicProtocolErrorResponse notImplemented(String message) {
        return new AnthropicProtocolErrorResponse("error", new Error("not_implemented_error", message));
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Internal server error\"}}";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private String type;
        private String message;
    }
}
