package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UsageExtractorTest {

    private UsageExtractor extractor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        extractor = new UsageExtractor(objectMapper);
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
        assertEquals(0, usage.getCacheHitTokens());
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
        assertEquals(0, usage.getCacheHitTokens());
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
        assertEquals(0, usage.getCacheHitTokens());
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
        assertEquals(0, usage.getCacheHitTokens());
        assertEquals(5, usage.getOutputTokens());
        assertEquals(13, usage.getTotalTokens());
    }

    @Test
    void shouldExtractUsageFromStructuredStreamEvent() {
        StreamEvent event = StreamEvent.builder()
                .event("response.completed")
                .dataLine("""
                        {"response":{"usage":{"input_tokens":9,"output_tokens":4,"total_tokens":13}}}
                        """.trim())
                .build();

        UsageMetrics usage = extractor.extract(event);

        assertNotNull(usage);
        assertEquals(9, usage.getInputTokens());
        assertEquals(0, usage.getCacheHitTokens());
        assertEquals(4, usage.getOutputTokens());
        assertEquals(13, usage.getTotalTokens());
    }

    @Test
    void shouldExtractCacheHitTokensForOpenAIStrategy() {
        String payload = """
                {
                  "usage": {
                    "input_tokens": 100,
                    "output_tokens": 20,
                    "total_tokens": 120,
                    "input_tokens_details": {
                      "cached_tokens": 40
                    }
                  }
                }
                """;

        UsageMetrics usage = extractor.extract(payload, "openai_cached_tokens");

        assertNotNull(usage);
        assertEquals(100, usage.getInputTokens());
        assertEquals(40, usage.getCacheHitTokens());
        assertEquals(20, usage.getOutputTokens());
        assertEquals(120, usage.getTotalTokens());
    }

    @Test
    void shouldClampCacheHitTokensToInputTokens() {
        String payload = """
                {
                  "usage": {
                    "prompt_tokens": 32,
                    "completion_tokens": 6,
                    "prompt_tokens_details": {
                      "cached_tokens": 80
                    }
                  }
                }
                """;

        UsageMetrics usage = extractor.extract(payload, "openai_cached_tokens");

        assertNotNull(usage);
        assertEquals(32, usage.getInputTokens());
        assertEquals(32, usage.getCacheHitTokens());
        assertEquals(6, usage.getOutputTokens());
        assertEquals(38, usage.getTotalTokens());
    }
}

