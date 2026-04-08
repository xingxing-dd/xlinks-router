package site.xlinks.ai.router.client.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fulfillment execution result.
 */
@Getter
@AllArgsConstructor
public class OrderFulfillmentResult {

    private final boolean fulfilled;
    private final String orderType;
    private final Long customerPlanId;
    private final String message;

    public static OrderFulfillmentResult fulfilled(String orderType, Long customerPlanId, String message) {
        return new OrderFulfillmentResult(true, orderType, customerPlanId, message);
    }

    public static OrderFulfillmentResult skipped(String orderType, String message) {
        return new OrderFulfillmentResult(false, orderType, null, message);
    }
}

