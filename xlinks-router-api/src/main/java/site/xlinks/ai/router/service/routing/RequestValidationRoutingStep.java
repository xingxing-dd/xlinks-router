package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.dto.ProxyRequest;

/**
 * Validates the basic request envelope before routing work starts.
 */
@Component
@RequiredArgsConstructor
public class RequestValidationRoutingStep implements RoutingStep {

    @Override
    public void apply(RoutingBuildContext context) {
        ProxyRequest request = context.getRequest();
        if (request == null) {
            throw ProxyErrors.requestMustNotBeNull();
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw ProxyErrors.modelMustNotBeBlank();
        }
        if (request.getProtocol() == null) {
            throw ProxyErrors.unsupportedProtocol();
        }
    }
}
