package site.xlinks.ai.router.distributed.domain.routing.filter;

import site.xlinks.ai.router.distributed.domain.routing.model.RoutingFilterContext;

public interface RoutingFilter {

    void filter(RoutingFilterContext context);
}
