package site.xlinks.ai.router.dto;

/**
 * Supported OpenAI-compatible upstream protocols.
 */
public enum OpenAIProtocol {

    CHAT_COMPLETIONS("/chat/completions", "chatcmpl-"),
    RESPONSES("/responses", "resp_");

    private final String providerPath;
    private final String requestIdPrefix;

    OpenAIProtocol(String providerPath, String requestIdPrefix) {
        this.providerPath = providerPath;
        this.requestIdPrefix = requestIdPrefix;
    }

    public String getProviderPath() {
        return providerPath;
    }

    public String getRequestIdPrefix() {
        return requestIdPrefix;
    }
}
