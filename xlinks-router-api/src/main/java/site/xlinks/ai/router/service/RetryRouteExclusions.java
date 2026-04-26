package site.xlinks.ai.router.service;

import site.xlinks.ai.router.context.ProviderInvokeContext;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks providers/tokens that should be avoided by follow-up retry attempts.
 */
public class RetryRouteExclusions {

    private final Set<Long> providerIds = new LinkedHashSet<>();
    private final Set<Long> providerTokenIds = new LinkedHashSet<>();

    public Set<Long> getProviderIds() {
        return providerIds;
    }

    public Set<Long> getProviderTokenIds() {
        return providerTokenIds;
    }

    public void exclude(ProviderInvokeContext context) {
        if (context == null) {
            return;
        }
        if (context.getProviderId() != null) {
            providerIds.add(context.getProviderId());
        }
        if (context.getProviderTokenId() != null) {
            providerTokenIds.add(context.getProviderTokenId());
        }
    }
}
