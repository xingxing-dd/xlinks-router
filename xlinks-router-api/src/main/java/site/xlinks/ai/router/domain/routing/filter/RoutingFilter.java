package site.xlinks.ai.router.domain.routing.filter;

import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;

public interface RoutingFilter {

    void filter(RoutingFilterContext context);
}
