package site.xlinks.ai.router.distributed.protocol.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public enum ForwardProtocol {

    COMPLETIONS("completions", "/v1/completions"),
    CHAT_COMPLETIONS("chat/completions", "/v1/chat/completions"),
    RESPONSES("responses", "/v1/responses"),
    MODELS("models", "/v1/models"),
    ANTHROPIC_MESSAGES("anthropic/messages", "/v1/messages");

    private final String code;
    private final String providerPath;

    private static final Map<String, ForwardProtocol> PROTOCOL_BY_PATH = buildProtocolByPath();

    ForwardProtocol(String code, String providerPath) {
        this.code = code;
        this.providerPath = providerPath;
    }

    public static Optional<ForwardProtocol> fromRequestUri(String requestUri) {
        return Optional.ofNullable(PROTOCOL_BY_PATH.get(requestUri));
    }

    private static Map<String, ForwardProtocol> buildProtocolByPath() {
        Map<String, ForwardProtocol> mappings = new HashMap<>();
        for (ForwardProtocol protocol : values()) {
            ForwardProtocol existing = mappings.putIfAbsent(protocol.providerPath, protocol);
            if (existing != null) {
                throw new IllegalStateException("Duplicate providerPath registered for " + protocol.providerPath);
            }
        }
        return Map.copyOf(mappings);
    }
}
