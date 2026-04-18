package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ProviderCacheHitStrategy;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;

/**
 * Extracts token usage from different OpenAI-compatible payload shapes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageExtractor {

    private final ObjectMapper objectMapper;

    public UsageMetrics extract(JsonNode payload) {
        return extract(payload, null);
    }

    public UsageMetrics extract(JsonNode payload, String modelProvider) {
        JsonNode usageNode = findUsageNode(payload);
        return extractFromUsageNode(usageNode, modelProvider);
    }

    public UsageMetrics extract(String payload) {
        return extract(payload, null);
    }

    public UsageMetrics extract(String payload, String modelProvider) {
        if (payload == null || payload.isBlank() || "[DONE]".equals(payload)) {
            return null;
        }

        try {
            return extract(objectMapper.readTree(payload), modelProvider);
        } catch (Exception e) {
            UsageMetrics fallbackUsage = extractUsageFromMalformedPayload(payload, modelProvider);
            if (fallbackUsage != null) {
                log.debug("Recovered usage metrics from malformed payload");
                return fallbackUsage;
            }
            log.debug("Failed to parse usage payload", e);
            return null;
        }
    }

    public UsageMetrics extract(StreamEvent event) {
        return extract(event, null);
    }

    public UsageMetrics extract(StreamEvent event, String modelProvider) {
        if (event == null || !event.hasData()) {
            return null;
        }
        return extract(event.joinedData(), modelProvider);
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

    private UsageMetrics extractFromUsageNode(JsonNode usageNode, String modelProvider) {
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
                ProviderCacheHitStrategy.fromModelProvider(modelProvider)
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
            if (field == null || field.isNull()) {
                continue;
            }
            if (field.canConvertToInt()) {
                return field.asInt();
            }
            if (field.isNumber() && field.canConvertToLong()) {
                long value = field.asLong();
                if (value > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                if (value < Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
                return (int) value;
            }
            if (field.isTextual()) {
                Integer parsed = parseIntSafely(field.asText());
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private Integer readNestedInt(JsonNode node, String objectFieldName, String fieldName) {
        JsonNode objectNode = node.get(objectFieldName);
        if (objectNode == null || objectNode.isNull()) {
            return null;
        }
        return readInt(objectNode, fieldName);
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

    private Integer parseIntSafely(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            long value = Long.parseLong(trimmed);
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (value < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UsageMetrics extractUsageFromMalformedPayload(String payload, String modelProvider) {
        String usageJson = extractUsageJsonObject(payload);
        if (usageJson == null || usageJson.isBlank()) {
            return null;
        }
        try {
            JsonNode usageNode = objectMapper.readTree(usageJson);
            return extractFromUsageNode(usageNode, modelProvider);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUsageJsonObject(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        int usageKeyIndex = payload.lastIndexOf("\"usage\"");
        if (usageKeyIndex < 0) {
            return null;
        }
        int colonIndex = payload.indexOf(':', usageKeyIndex);
        if (colonIndex < 0) {
            return null;
        }
        int objectStartIndex = payload.indexOf('{', colonIndex);
        if (objectStartIndex < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = objectStartIndex; i < payload.length(); i++) {
            char current = payload.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                    continue;
                }
                if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return payload.substring(objectStartIndex, i + 1);
                }
            }
        }
        return null;
    }
}

