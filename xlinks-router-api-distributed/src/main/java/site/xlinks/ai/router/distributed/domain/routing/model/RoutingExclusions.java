package site.xlinks.ai.router.distributed.domain.routing.model;

import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 路由排除集。
 * 用于在重试过程中排除已经失败的服务商或服务商令牌。
 */
@EqualsAndHashCode
public class RoutingExclusions {

    private final Set<Long> excludedProviderIds = new LinkedHashSet<>();
    private final Set<Long> excludedProviderTokenIds = new LinkedHashSet<>();

    public static RoutingExclusions none() {
        return new RoutingExclusions();
    }

    public static RoutingExclusions create() {
        return new RoutingExclusions();
    }

    public Set<Long> getExcludedProviderIds() {
        return Collections.unmodifiableSet(excludedProviderIds);
    }

    public Set<Long> getExcludedProviderTokenIds() {
        return Collections.unmodifiableSet(excludedProviderTokenIds);
    }

    public boolean isProviderExcluded(Long providerId) {
        return providerId != null && excludedProviderIds.contains(providerId);
    }

    public boolean isProviderTokenExcluded(Long providerTokenId) {
        return providerTokenId != null && excludedProviderTokenIds.contains(providerTokenId);
    }

    public void excludeProviderId(Long providerId) {
        if (providerId != null) {
            excludedProviderIds.add(providerId);
        }
    }

    public void excludeProviderTokenId(Long providerTokenId) {
        if (providerTokenId != null) {
            excludedProviderTokenIds.add(providerTokenId);
        }
    }
}
