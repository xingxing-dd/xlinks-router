package site.xlinks.ai.router.distributed.protocol.model;

public enum ForwardProtocol {

    COMPLETIONS("completions", "/v1/completions"),
    CHAT_COMPLETIONS("chat/completions", "/v1/chat/completions"),
    MODELS("models", "/v1/models"),
    ANTHROPIC_MESSAGES("anthropic/messages", "/v1/messages");

    private final String code;
    private final String providerPath;

    ForwardProtocol(String code, String providerPath) {
        this.code = code;
        this.providerPath = providerPath;
    }

    public String getCode() {
        return code;
    }

    public String getProviderPath() {
        return providerPath;
    }
}
