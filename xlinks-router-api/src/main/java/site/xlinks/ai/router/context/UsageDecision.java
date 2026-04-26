package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Usage decision resolved for the current customer token request.
 */
@Data
@Builder
public class UsageDecision {

    private Long customerTokenId;

    private String customerName;

    /**
     * Customer plan record id.
     */
    private Long planId;

    private String planName;

    private boolean packageEnabled;

    private boolean balanceEnabled;

    /**
     * 0: package priority, 1: package only, 2: balance only.
     */
    private Integer currentUsageType;

    private List<String> packageAllowedModels;

    private boolean unlimited;

    /**
     * Cache-hit billing multiplier from the selected customer plan snapshot.
     */
    private BigDecimal multiplier;
}
