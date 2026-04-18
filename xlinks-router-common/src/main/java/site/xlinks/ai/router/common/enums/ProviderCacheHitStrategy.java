package site.xlinks.ai.router.common.enums;

import java.util.Locale;

/**
 * Provider cache-hit token accounting strategies.
 */
public enum ProviderCacheHitStrategy {
    /**
     * Provider does not expose cache-hit token details.
     */
    NONE("none", false),
    /**
     * OpenAI-compatible payload:
     * usage.prompt_tokens_details.cached_tokens / usage.input_tokens_details.cached_tokens
     */
    OPENAI_CACHED_TOKENS("openai_cached_tokens", true),
    /**
     * Anthropic-style payload:
     * usage.cache_read_input_tokens
     */
    ANTHROPIC_CACHE_READ_INPUT_TOKENS("anthropic_cache_read_input_tokens", true);

    private final String code;
    private final boolean cacheHitSupported;

    ProviderCacheHitStrategy(String code, boolean cacheHitSupported) {
        this.code = code;
        this.cacheHitSupported = cacheHitSupported;
    }

    public String getCode() {
        return code;
    }

    public boolean isCacheHitSupported() {
        return cacheHitSupported;
    }

    public static ProviderCacheHitStrategy fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (ProviderCacheHitStrategy value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return NONE;
    }

    public static ProviderCacheHitStrategy fromModelProvider(String modelProvider) {
        if (modelProvider == null || modelProvider.isBlank()) {
            return NONE;
        }
        String normalized = modelProvider.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("OPENAI")) {
            return OPENAI_CACHED_TOKENS;
        }
        if (normalized.contains("ANTHROPIC")) {
            return ANTHROPIC_CACHE_READ_INPUT_TOKENS;
        }
        return NONE;
    }
}
