package site.xlinks.ai.router.anthropic.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Anthropic-compatible error payload:
 * {
 *   "type": "error",
 *   "error": {
 *     "type": "invalid_request_error",
 *     "message": "..."
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnthropicErrorResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String type;
    private Error error;

    public static AnthropicErrorResponse invalidRequest(String message) {
        return new AnthropicErrorResponse("error", new Error("invalid_request_error", message));
    }

    public static AnthropicErrorResponse authenticationError(String message) {
        return new AnthropicErrorResponse("error", new Error("authentication_error", message));
    }

    public static AnthropicErrorResponse permissionError(String message) {
        return new AnthropicErrorResponse("error", new Error("permission_error", message));
    }

    public static AnthropicErrorResponse apiError(String message) {
        return new AnthropicErrorResponse("error", new Error("api_error", message));
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
