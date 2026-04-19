package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.service.CustomerTokenAuthService;

/**
 * Validates customer token access for the requested model.
 */
@Component
@RequiredArgsConstructor
public class CustomerTokenRoutingStep implements RoutingStep {

    private final CustomerTokenAuthService customerTokenAuthService;

    @Override
    public void apply(RoutingBuildContext context) {
        CustomerToken customerToken = customerTokenAuthService.validateRequestAccess(
                context.getToken(),
                context.getRequest().getModel()
        );
        context.setCustomerToken(customerToken);
    }
}
