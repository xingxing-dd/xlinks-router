package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAIUsageExtractorTest {

    private OpenAIUsageExtractor extractor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        extractor = new OpenAIUsageExtractor(objectMapper);
    }

    @Test
    void shouldExtractChatCompletionUsage() throws Exception {
        String payload = """
                {
                  "id": "chatcmpl-test",
                  "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 34,
                    "total_tokens": 46
                  }
                }
                """;

        UsageMetrics usage = extractor.extract(objectMapper.readTree(payload));

        assertNotNull(usage);
        assertEquals(12, usage.getInputTokens());
        assertEquals(34, usage.getOutputTokens());
        assertEquals(46, usage.getTotalTokens());
    }

    @Test
    void shouldExtractResponsesUsageFromTopLevelPayload() throws Exception {
        String payload = """
                {
                  "id": "resp_test",
                  "usage": {
                    "input_tokens": 20,
                    "output_tokens": 10,
                    "total_tokens": 30
                  }
                }
                """;

        UsageMetrics usage = extractor.extract(objectMapper.readTree(payload));

        assertNotNull(usage);
        assertEquals(20, usage.getInputTokens());
        assertEquals(10, usage.getOutputTokens());
        assertEquals(30, usage.getTotalTokens());
    }

    @Test
    void shouldExtractResponsesUsageFromCompletedStreamEvent() {
        String payload = """
                {
                  "type": "response.completed",
                  "response": {
                    "id": "resp_test",
                    "usage": {
                      "input_tokens": 28,
                      "output_tokens": 14,
                      "total_tokens": 42
                    }
                  }
                }
                """;

        UsageMetrics usage = extractor.extract(payload);

        assertNotNull(usage);
        assertEquals(28, usage.getInputTokens());
        assertEquals(14, usage.getOutputTokens());
        assertEquals(42, usage.getTotalTokens());
    }

    @Test
    void shouldIgnoreDoneAndNonUsagePayloads() {
        assertNull(extractor.extract("[DONE]"));
        assertNull(extractor.extract("{\"type\":\"response.output_text.delta\",\"delta\":\"hi\"}"));
    }

    @Test
    void shouldBackfillTotalTokensWhenMissing() {
        String payload = """
                {
                  "usage": {
                    "input_tokens": 8,
                    "output_tokens": 5
                  }
                }
                """;

        UsageMetrics usage = extractor.extract(payload);

        assertNotNull(usage);
        assertEquals(8, usage.getInputTokens());
        assertEquals(5, usage.getOutputTokens());
        assertEquals(13, usage.getTotalTokens());
    }

    @Test
    void shouldExtractUsageFromStructuredStreamEvent() {
        OpenAIStreamEvent event = OpenAIStreamEvent.builder()
                .event("response.completed")
                .dataLine("""
                        {"response":{"usage":{"input_tokens":9,"output_tokens":4,"total_tokens":13}}}
                        """.trim())
                .build();

        UsageMetrics usage = extractor.extract(event);

        assertNotNull(usage);
        assertEquals(9, usage.getInputTokens());
        assertEquals(4, usage.getOutputTokens());
        assertEquals(13, usage.getTotalTokens());
    }
}
