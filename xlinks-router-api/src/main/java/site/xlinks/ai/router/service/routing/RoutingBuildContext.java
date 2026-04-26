package site.xlinks.ai.router.service.routing;

import lombok.Data;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.ProviderPermitLease;

import java.util.Set;

/**
 * Mutable state shared across routing pipeline steps.
 */
@Data
public class RoutingBuildContext {

    private final String token;
    private final ProxyRequest request;
    private final String requestId;
    private final Set<Long> excludedProviderIds;
    private final Set<Long> excludedProviderTokenIds;

    private CustomerToken customerToken;
    private UsageDecision usageDecision;
    private Model model;
    private Provider provider;
    private ProviderModel providerModel;
    private ProviderToken providerToken;
    private ProviderPermitLease providerPermitLease;
}
