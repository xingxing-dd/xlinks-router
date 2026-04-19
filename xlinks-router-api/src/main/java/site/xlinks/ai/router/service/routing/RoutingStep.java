package site.xlinks.ai.router.service.routing;

/**
 * One step in the proxy routing pipeline.
 */
public interface RoutingStep {

    void apply(RoutingBuildContext context);
}
