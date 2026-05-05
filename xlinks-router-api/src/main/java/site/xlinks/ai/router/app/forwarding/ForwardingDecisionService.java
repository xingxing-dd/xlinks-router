package site.xlinks.ai.router.app.forwarding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.app.forwarding.model.ForwardingNextAction;
import site.xlinks.ai.router.app.forwarding.model.ForwardingStage;
import site.xlinks.ai.router.domain.routing.DefaultRoutingDomainService;
import site.xlinks.ai.router.domain.routing.model.RoutingPlan;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

@Service
@RequiredArgsConstructor
public class ForwardingDecisionService {

    private final DefaultRoutingDomainService routingDomainService;

    public ForwardingDecision decide(ForwardRequest request, String requestId) {
        RoutingPlan routingPlan = routingDomainService.route(request);
        return ForwardingDecision.builder()
                .request(request)
                .customerToken(routingPlan.getCustomerToken())
                .customerAccount(routingPlan.getCustomerAccount())
                .customerPlan(routingPlan.getCustomerPlan())
                .model(routingPlan.getModel())
                .requestId(requestId)
                .consumptionMode(routingPlan.getConsumptionMode())
                .routingPlan(routingPlan)
                .stage(ForwardingStage.ROUTING_DECIDED)
                .nextAction(ForwardingNextAction.TARGET_SELECTION)
                .build();
    }
}
