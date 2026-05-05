package site.xlinks.ai.router.app.forwarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMetrics {

    private Integer inputTokens;

    private Integer cacheHitTokens;

    private Integer outputTokens;

    private Integer totalTokens;
}
