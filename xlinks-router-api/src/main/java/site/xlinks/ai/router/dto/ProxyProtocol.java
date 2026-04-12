package site.xlinks.ai.router.dto;

import java.util.Locale;

/**
 * Supported OpenAI-compatible upstream protocols.
 */
public enum ProxyProtocol {

    CHAT_COMPLETIONS("chat/completions", "/chat/completions", "chatcmpl-"),
    RESPONSES("responses", "/responses", "resp_"),
    ANTHROPIC_MESSAGES("anthropic/messages", "/messages", "msg_");

    private final String code;
    private final String providerPath;
    private final String requestIdPrefix;

    ProxyProtocol(String code, String providerPath, String requestIdPrefix) {
        this.code = code;
        this.providerPath = providerPath;
        this.requestIdPrefix = requestIdPrefix;
    }

    public String getCode() {
        return code;
    }

    public String getProviderPath() {
        return providerPath;
    }

    public String getRequestIdPrefix() {
        return requestIdPrefix;
    }

    public boolean matches(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalize(value);
        return normalized.equals(normalize(code))
                || normalized.equals(normalize(providerPath))
                || normalized.equals(normalize(name()));
    }

    private String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}

