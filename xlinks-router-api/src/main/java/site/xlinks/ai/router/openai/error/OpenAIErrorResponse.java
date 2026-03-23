package site.xlinks.ai.router.openai.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal OpenAI-compatible error payload.
 *
 * {
 *   "error": {
 *     "message": "...",
 *     "type": "...",
 *     "param": null,
 *     "code": "..."
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIErrorResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private Error error;

    public static OpenAIErrorResponse unauthorized(String message) {
        return new OpenAIErrorResponse(new Error(message, "invalid_request_error", null, "invalid_api_key"));
    }

    public static OpenAIErrorResponse invalidRequest(String message) {
        return new OpenAIErrorResponse(new Error(message, "invalid_request_error", null, "invalid_request"));
    }

    public static OpenAIErrorResponse internalError(String message) {
        return new OpenAIErrorResponse(new Error(message, "server_error", null, "internal_error"));
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // Last resort: never throw from error rendering.
            return "{\"error\":{\"message\":\"Serialization error\",\"type\":\"server_error\",\"code\":\"internal_error\"}}";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private String message;
        private String type;
        private String param;
        private String code;
    }
}
