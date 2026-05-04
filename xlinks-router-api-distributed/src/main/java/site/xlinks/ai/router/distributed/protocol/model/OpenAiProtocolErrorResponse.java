package site.xlinks.ai.router.distributed.protocol.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiProtocolErrorResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private Error error;

    public static OpenAiProtocolErrorResponse unauthorized(String message) {
        return new OpenAiProtocolErrorResponse(new Error(message, "invalid_request_error", null, "invalid_api_key"));
    }

    public static OpenAiProtocolErrorResponse invalidRequest(String message) {
        return new OpenAiProtocolErrorResponse(new Error(message, "invalid_request_error", null, "invalid_request"));
    }

    public static OpenAiProtocolErrorResponse rateLimited(String message) {
        return new OpenAiProtocolErrorResponse(new Error(message, "rate_limit_error", null, "rate_limit_exceeded"));
    }

    public static OpenAiProtocolErrorResponse internalError(String message) {
        return new OpenAiProtocolErrorResponse(new Error(message, "server_error", null, "internal_error"));
    }

    public static OpenAiProtocolErrorResponse notImplemented(String message) {
        return new OpenAiProtocolErrorResponse(new Error(message, "server_error", null, "not_implemented"));
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
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
