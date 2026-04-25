package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.service.ProxyRequestTrace;

/**
 * Selects the concrete provider, provider-model mapping, and provider token.
 */
@Component
@RequiredArgsConstructor
public class ProviderRouteSelectionStep implements RoutingStep {

    private final ProviderRouteResolver providerRouteResolver;

    @Override
    public void apply(RoutingBuildContext context) {
        ProviderRouteResolver.ResolvedProviderRoute route = providerRouteResolver.resolve(
                context.getCustomerToken().getAccountId(),
                context.getModel().getId(),
                context.getModel().getModelCode(),
                context.getRequest().getProtocol(),
                context.getRequestId()
        );
        context.setProvider(route.provider());
        context.setProviderModel(route.providerModel());
        context.setProviderToken(route.providerToken());
        context.setProviderPermitLease(route.providerPermitLease());
        ProxyRequestTrace.markRouteResolved(route.provider(), route.providerModel(), route.providerToken(), route.providerPermitLease());
        ProxyRequestTrace.addRouteEvent("已选定 provider(providerId=" + route.provider().getId()
                + ", providerTokenId=" + route.providerToken().getId() + ")");
    }
}
