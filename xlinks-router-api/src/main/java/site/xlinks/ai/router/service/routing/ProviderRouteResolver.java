package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.ProviderTokenSelectService;
import site.xlinks.ai.router.service.RouteCacheService;

import java.util.List;

/**
 * Resolves the first available provider route for a model/protocol pair.
 */
@Service
@RequiredArgsConstructor
public class ProviderRouteResolver {

    private final RouteCacheService routeCacheService;
    private final ProviderTokenSelectService providerTokenSelectService;

    public ResolvedProviderRoute resolve(Long modelId, String modelCode, ProxyProtocol protocol) {
        List<ProviderModel> providerModels = routeCacheService.listProviderModelsByPriority(modelId, protocol);
        if (providerModels == null || providerModels.isEmpty()) {
            throw ProxyErrors.noProviderMapping(modelCode);
        }

        for (ProviderModel candidate : providerModels) {
            if (candidate == null || candidate.getProviderId() == null) {
                continue;
            }
            Provider candidateProvider = routeCacheService.getProvider(candidate.getProviderId());
            if (candidateProvider == null || candidateProvider.getStatus() == null || candidateProvider.getStatus() != 1) {
                continue;
            }
            ProviderToken candidateToken = providerTokenSelectService.selectTokenOrNull(candidateProvider.getId());
            if (candidateToken == null) {
                continue;
            }
            return new ResolvedProviderRoute(candidateProvider, candidate, candidateToken);
        }

        throw ProxyErrors.noProviderToken(modelCode);
    }

    public record ResolvedProviderRoute(Provider provider, ProviderModel providerModel, ProviderToken providerToken) {
    }
}
