package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.service.ProxyRequestTrace;
import site.xlinks.ai.router.service.RouteCacheService;

/**
 * Resolves the customer-requested model from the route cache.
 */
@Component
@RequiredArgsConstructor
public class ModelResolutionRoutingStep implements RoutingStep {

    private final RouteCacheService routeCacheService;

    @Override
    public void apply(RoutingBuildContext context) {
        Model model = routeCacheService.getModel(context.getRequest().getModel());
        if (model == null) {
            throw ProxyErrors.modelUnavailable(context.getRequest().getModel());
        }
        context.setModel(model);
        ProxyRequestTrace.markModel(model);
        ProxyRequestTrace.addRouteEvent("模型解析完成(modelId=" + model.getId()
                + ", modelCode=" + model.getModelCode()
                + ", modelName=" + model.getModelName() + ")");
    }
}
