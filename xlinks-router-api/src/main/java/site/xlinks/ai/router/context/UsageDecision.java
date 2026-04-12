package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 鏉冪泭鍒ゅ畾缁撴灉
 * 鐢ㄤ簬鍒ゆ柇褰撳墠璇锋眰搴旇浣跨敤濂楅杩樻槸浣欓
 */
@Data
@Builder
public class UsageDecision {

    /**
     * 瀹㈡埛 Token ID
     */
    private Long customerTokenId;

    /**
     * 瀹㈡埛鍚嶇О
     */
    private String customerName;

    /**
     * 鍙敤濂楅 ID
     */
    private Long planId;

    /**
     * 濂楅鏄惁鍚敤
     */
    private boolean packageEnabled;

    /**
     * 浣欓鏄惁鍚敤
     */
    private boolean balanceEnabled;

    /**
     * 褰撳墠浣跨敤绫诲瀷锛?-涓嶉檺鍒讹紝1-浠呭椁愶紝2-浠呬綑棰?
     * 鏍规嵁濂楅鍜屼綑棰濈殑鐘舵€佺患鍚堣绠?
     */
    private Integer currentUsageType;

    /**
     * 濂楅鍏佽鐨勬ā鍨嬪垪琛紙JSON鏍煎紡锛?
     */
    private List<String> packageAllowedModels;

    /**
     * 鏄惁鏃犻檺鏉冮檺锛坢ock妯″紡锛?
     */
    private boolean unlimited;
}
