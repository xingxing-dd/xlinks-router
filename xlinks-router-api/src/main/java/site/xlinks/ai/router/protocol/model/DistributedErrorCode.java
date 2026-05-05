package site.xlinks.ai.router.protocol.model;

import lombok.Getter;

@Getter
public enum DistributedErrorCode {

    INVALID_JSON_REQUEST(4600, "Invalid JSON request body"),
    MODEL_REQUIRED(4601, "Model is required"),
    MISSING_CUSTOMER_TOKEN(4602, "Missing customer token"),
    INVALID_AUTHORIZATION_HEADER(4603, "Invalid Authorization header"),
    PROTOCOL_DEFINITION_ONLY(4604, "Protocol endpoint is defined, routing/forwarding is not connected yet");

    private final int code;
    private final String message;

    DistributedErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
