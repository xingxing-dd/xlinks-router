package site.xlinks.ai.router.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TokenUsageStats {
    private String customerToken;
    private Long totalRequests;
    private LocalDateTime lastUsedAt;
}
