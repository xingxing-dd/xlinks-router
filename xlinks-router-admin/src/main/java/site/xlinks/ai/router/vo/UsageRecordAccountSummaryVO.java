package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Usage record summary grouped by account")
public class UsageRecordAccountSummaryVO {

    private Long accountId;
    private String accountName;
    private String accountPhone;
    private String accountEmail;
    private Long requestCount;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long cacheHitTokens;
    private BigDecimal totalCost;
    private BigDecimal avgSessionMs;
}
