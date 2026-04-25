package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.service.ProxyRequestTrace;

import java.util.List;

/**
 * Ordered routing pipeline used to build provider invocation context.
 */
@Service
@RequiredArgsConstructor
public class ProxyRoutingPipeline {

    private final RequestValidationRoutingStep requestValidationRoutingStep;
    private final CustomerTokenRoutingStep customerTokenRoutingStep;
    private final UsageDecisionRoutingStep usageDecisionRoutingStep;
    private final ModelResolutionRoutingStep modelResolutionRoutingStep;
    private final ProviderRouteSelectionStep providerRouteSelectionStep;

    public RoutingBuildContext resolve(String token, ProxyRequest request, String requestId) {
        RoutingBuildContext context = new RoutingBuildContext(token, request, requestId);
        List<RoutingStep> steps = List.of(
                requestValidationRoutingStep,
                customerTokenRoutingStep,
                usageDecisionRoutingStep,
                modelResolutionRoutingStep,
                providerRouteSelectionStep
        );
        for (RoutingStep step : steps) {
            ProxyRequestTrace.addRouteEvent("执行路由步骤: " + step.getClass().getSimpleName());
            step.apply(context);
        }
        return context;
    }
}
