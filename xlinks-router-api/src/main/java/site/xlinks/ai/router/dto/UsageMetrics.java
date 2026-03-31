package site.xlinks.ai.router.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified token usage metrics across different OpenAI-compatible protocols.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMetrics {

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;
}
