package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.service.ProxyRequestTrace;
import site.xlinks.ai.router.service.UsageEntitlementService;

/**
 * Resolves the active entitlement and package availability for the request.
 */
@Component
@RequiredArgsConstructor
public class UsageDecisionRoutingStep implements RoutingStep {

    private final UsageEntitlementService usageEntitlementService;

    @Override
    public void apply(RoutingBuildContext context) {
        UsageDecision usageDecision = usageEntitlementService.decide(
                context.getCustomerToken(),
                context.getRequest().getModel()
        );
        if (usageDecision == null || (!usageDecision.isPackageEnabled() && !usageDecision.isBalanceEnabled())) {
            throw ProxyErrors.noUsableEntitlement();
        }
        context.setUsageDecision(usageDecision);
        ProxyRequestTrace.markUsageDecision(usageDecision);
        ProxyRequestTrace.addRouteEvent("权益决策完成(planId=" + usageDecision.getPlanId()
                + ", currentUsageType=" + usageDecision.getCurrentUsageType()
                + ", packageEnabled=" + usageDecision.isPackageEnabled()
                + ", balanceEnabled=" + usageDecision.isBalanceEnabled() + ")");
    }
}
