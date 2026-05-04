package site.xlinks.ai.router.distributed.app.forwarding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.distributed.domain.routing.RoutingDomainService;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingContext;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.distributed.infrastructure.http.ProviderHttpForwardingExecutor;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingPreparation;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingApplicationService {

    private final ForwardingReadModelLoader forwardingReadModelLoader;
    private final RoutingDomainService routingDomainService;
    private final ProviderHttpForwardingExecutor providerHttpForwardingExecutor;

    public ForwardingPreparation prepare(ForwardRequest request) {
        CustomerToken customerToken = forwardingReadModelLoader.loadCustomerToken(request.getCustomerToken());
        CustomerAccount customerAccount = forwardingReadModelLoader.loadCustomerAccount(customerToken.getAccountId());
        CustomerPlan customerPlan = forwardingReadModelLoader.loadActivePlan(customerAccount.getId());
        Model model = forwardingReadModelLoader.loadModel(request.getModel());

        log.debug("Forwarding preparation completed. customerTokenId={}, accountId={}, planId={}, modelId={}, protocol={}",
                customerToken.getId(),
                customerAccount.getId(),
                customerPlan == null ? null : customerPlan.getId(),
                model.getId(),
                request.getProtocol().getCode());

        return ForwardingPreparation.builder()
                .request(request)
                .customerToken(customerToken)
                .customerAccount(customerAccount)
                .customerPlan(customerPlan)
                .model(model)
                .stage("FORWARDING_PREPARED")
                .nextAction("ROUTING_DECISION")
                .build();
    }

    public Object forward(ForwardRequest request) {
        ForwardingPreparation preparation = prepare(request);
        RoutingDecision routingDecision = routingDomainService.route(
                RoutingContext.builder()
                        .request(preparation.getRequest())
                        .customerToken(preparation.getCustomerToken())
                        .customerAccount(preparation.getCustomerAccount())
                        .customerPlan(preparation.getCustomerPlan())
                        .model(preparation.getModel())
                        .build()
        );
        preparation.setRoutingDecision(routingDecision);
        preparation.setStage("ROUTED");
        preparation.setNextAction("HTTP_FORWARDING");
        return providerHttpForwardingExecutor.forward(preparation);
    }
}
