package site.xlinks.ai.router.client.service;

import site.xlinks.ai.router.entity.CustomerOrder;

/**
 * Strategy for handling different paid order types.
 */
public interface OrderFulfillmentStrategy {

    /**
     * Supported order type.
     */
    String orderType();

    /**
     * Fulfill paid order business.
     */
    OrderFulfillmentResult fulfill(CustomerOrder order);
}

