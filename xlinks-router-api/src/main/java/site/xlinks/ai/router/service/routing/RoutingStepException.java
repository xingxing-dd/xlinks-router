package site.xlinks.ai.router.service.routing;

import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * Business exception that preserves the partially-built routing context.
 */
public class RoutingStepException extends BusinessException {

    private final RoutingBuildContext routingContext;

    public RoutingStepException(RoutingBuildContext routingContext, BusinessException cause) {
        super(cause.getCode(), cause.getMessage());
        this.routingContext = routingContext;
        initCause(cause);
    }

    public RoutingStepException(RoutingBuildContext routingContext, RuntimeException cause) {
        super(ErrorCode.INTERNAL_ERROR, cause == null ? null : cause.getMessage());
        this.routingContext = routingContext;
        if (cause != null) {
            initCause(cause);
        }
    }

    public RoutingBuildContext getRoutingContext() {
        return routingContext;
    }
}
