package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ProviderCacheHitStrategy;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;

/**
 * Extracts token usage from different OpenAI-compatible payload shapes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIUsageExtractor {

    private final ObjectMapper objectMapper;

    public UsageMetrics extract(JsonNode payload) {
        return extract(payload, ProviderCacheHitStrategy.NONE.getCode());
    }

    public UsageMetrics extract(JsonNode payload, String cacheHitStrategyCode) {
        JsonNode usageNode = findUsageNode(payload);
        if (usageNode == null || usageNode.isMissingNode() || usageNode.isNull()) {
            return null;
        }

        Integer inputTokens = readInt(usageNode, "prompt_tokens", "input_tokens");
        Integer outputTokens = readInt(usageNode, "completion_tokens", "output_tokens");
        Integer totalTokens = readInt(usageNode, "total_tokens");

        if (inputTokens == null && outputTokens == null && totalTokens == null) {
            return null;
        }

        if (totalTokens == null) {
            totalTokens = defaultInt(inputTokens) + defaultInt(outputTokens);
        }

        Integer cacheHitTokens = extractCacheHitTokens(
                usageNode,
                ProviderCacheHitStrategy.fromCode(cacheHitStrategyCode)
        );
        int normalizedInput = defaultInt(inputTokens);
        int normalizedCacheHit = normalizeCacheHitTokens(cacheHitTokens, normalizedInput);

        return UsageMetrics.builder()
                .inputTokens(normalizedInput)
                .cacheHitTokens(normalizedCacheHit)
                .outputTokens(defaultInt(outputTokens))
                .totalTokens(defaultInt(totalTokens))
                .build();
    }

    public UsageMetrics extract(String payload) {
        return extract(payload, ProviderCacheHitStrategy.NONE.getCode());
    }

    public UsageMetrics extract(String payload, String cacheHitStrategyCode) {
        if (payload == null || payload.isBlank() || "[DONE]".equals(payload)) {
            return null;
        }

        try {
            return extract(objectMapper.readTree(payload), cacheHitStrategyCode);
        } catch (Exception e) {
            log.debug("Failed to parse usage payload", e);
            return null;
        }
    }

    public UsageMetrics extract(OpenAIStreamEvent event) {
        return extract(event, ProviderCacheHitStrategy.NONE.getCode());
    }

    public UsageMetrics extract(OpenAIStreamEvent event, String cacheHitStrategyCode) {
        if (event == null || !event.hasData()) {
            return null;
        }
        return extract(event.joinedData(), cacheHitStrategyCode);
    }

    private JsonNode findUsageNode(JsonNode payload) {
        if (payload == null || payload.isMissingNode()) {
            return null;
        }
        if (payload.has("usage")) {
            return payload.get("usage");
        }
        JsonNode responseNode = payload.get("response");
        if (responseNode != null && responseNode.has("usage")) {
            return responseNode.get("usage");
        }
        return null;
    }

    private Integer extractCacheHitTokens(JsonNode usageNode, ProviderCacheHitStrategy strategy) {
        if (usageNode == null || strategy == null || !strategy.isCacheHitSupported()) {
            return 0;
        }
        return switch (strategy) {
            case OPENAI_CACHED_TOKENS -> firstNonNull(
                    readNestedInt(usageNode, "prompt_tokens_details", "cached_tokens"),
                    readNestedInt(usageNode, "input_tokens_details", "cached_tokens"),
                    readInt(usageNode, "cached_tokens", "cache_hit_tokens")
            );
            case ANTHROPIC_CACHE_READ_INPUT_TOKENS -> firstNonNull(
                    readInt(usageNode, "cache_read_input_tokens"),
                    readNestedInt(usageNode, "input_tokens_details", "cache_read_tokens"),
                    readNestedInt(usageNode, "input_tokens_details", "cached_tokens")
            );
            default -> 0;
        };
    }

    private int normalizeCacheHitTokens(Integer cacheHitTokens, int inputTokens) {
        int value = defaultInt(cacheHitTokens);
        if (value < 0) {
            return 0;
        }
        if (inputTokens <= 0) {
            return 0;
        }
        return Math.min(value, inputTokens);
    }

    private Integer readInt(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && field.canConvertToInt()) {
                return field.asInt();
            }
        }
        return null;
    }

    private Integer readNestedInt(JsonNode node, String objectFieldName, String fieldName) {
        JsonNode objectNode = node.get(objectFieldName);
        if (objectNode == null || objectNode.isNull()) {
            return null;
        }
        JsonNode field = objectNode.get(fieldName);
        if (field != null && field.canConvertToInt()) {
            return field.asInt();
        }
        return null;
    }

    private Integer firstNonNull(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
