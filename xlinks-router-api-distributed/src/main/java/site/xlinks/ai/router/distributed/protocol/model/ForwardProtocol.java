package site.xlinks.ai.router.distributed.protocol.model;

public enum ForwardProtocol {

    COMPLETIONS("completions"),
    CHAT_COMPLETIONS("chat/completions"),
    MODELS("models"),
    ANTHROPIC_MESSAGES("anthropic/messages");

    private final String code;

    ForwardProtocol(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
