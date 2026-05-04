package site.xlinks.ai.router.vo;

import lombok.Data;

@Data
public class CustomerTokenUsageStatsVO {
    private String tokenHash;
    private Long todayUsedTokens;
    private Long totalUsedTokens;
}
