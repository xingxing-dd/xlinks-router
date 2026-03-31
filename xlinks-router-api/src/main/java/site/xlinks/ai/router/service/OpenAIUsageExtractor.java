package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

        return UsageMetrics.builder()
                .inputTokens(defaultInt(inputTokens))
                .outputTokens(defaultInt(outputTokens))
                .totalTokens(defaultInt(totalTokens))
                .build();
    }

    public UsageMetrics extract(String payload) {
        if (payload == null || payload.isBlank() || "[DONE]".equals(payload)) {
            return null;
        }

        try {
            return extract(objectMapper.readTree(payload));
        } catch (Exception e) {
            log.debug("Failed to parse usage payload", e);
            return null;
        }
    }

    public UsageMetrics extract(OpenAIStreamEvent event) {
        if (event == null || !event.hasData()) {
            return null;
        }
        return extract(event.joinedData());
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

    private Integer readInt(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && field.canConvertToInt()) {
                return field.asInt();
            }
        }
        return null;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
