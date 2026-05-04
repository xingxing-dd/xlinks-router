package site.xlinks.ai.router.distributed.domain.routing;

import site.xlinks.ai.router.distributed.domain.routing.model.RoutingContext;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingDecision;

public interface RoutingDomainService {

    RoutingDecision route(RoutingContext context);
}
