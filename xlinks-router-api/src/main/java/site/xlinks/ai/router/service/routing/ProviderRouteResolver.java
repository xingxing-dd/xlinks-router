package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.ProviderPermitLease;
import site.xlinks.ai.router.service.ProviderTokenSelectService;
import site.xlinks.ai.router.service.RouteCacheService;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the first available provider route for a model/protocol pair.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderRouteResolver {

    private final RouteCacheService routeCacheService;
    private final ProviderTokenSelectService providerTokenSelectService;

    public ResolvedProviderRoute resolve(Long accountId,
                                         Long modelId,
                                         String modelCode,
                                         ProxyProtocol protocol,
                                         String requestId) {
        List<ProviderModel> providerModels = routeCacheService.listProviderModelsByPriority(modelId, protocol);
        if (providerModels == null || providerModels.isEmpty()) {
            throw ProxyErrors.noProviderMapping(modelCode);
        }

        providerModels = prioritizeMerchantConfiguredProvider(accountId, modelId, providerModels);
        boolean concurrencyLimited = false;

        for (ProviderModel candidate : providerModels) {
            if (candidate == null || candidate.getProviderId() == null) {
                continue;
            }
            if (routeCacheService.isProviderTemporarilyUnavailable(candidate.getProviderId())) {
                log.warn("Skipping temporarily unavailable provider. providerId={}, modelId={}, modelCode={}, protocol={}",
                        candidate.getProviderId(), modelId, modelCode, protocol);
                continue;
            }
            Provider candidateProvider = routeCacheService.getProvider(candidate.getProviderId());
            if (candidateProvider == null || candidateProvider.getStatus() == null || candidateProvider.getStatus() != 1) {
                continue;
            }
            ProviderTokenSelectService.SelectionResult selectionResult =
                    providerTokenSelectService.selectTokenLeaseOrNull(candidateProvider, requestId);
            if (selectionResult.token() == null) {
                concurrencyLimited = concurrencyLimited || selectionResult.concurrencyLimited();
                continue;
            }
            return new ResolvedProviderRoute(
                    candidateProvider,
                    candidate,
                    selectionResult.token(),
                    selectionResult.lease()
            );
        }

        if (concurrencyLimited) {
            throw ProxyErrors.providerTokenRateLimited();
        }
        throw ProxyErrors.noProviderToken(modelCode);
    }

    private List<ProviderModel> prioritizeMerchantConfiguredProvider(Long accountId,
                                                                     Long modelId,
                                                                     List<ProviderModel> providerModels) {
        if (accountId == null || modelId == null || providerModels == null || providerModels.isEmpty()) {
            return providerModels;
        }
        Long preferredProviderId = routeCacheService.getMerchantPreferredProviderId(accountId, modelId);
        if (preferredProviderId == null) {
            return providerModels;
        }

        List<ProviderModel> preferred = new ArrayList<>();
        List<ProviderModel> others = new ArrayList<>();
        for (ProviderModel providerModel : providerModels) {
            if (providerModel == null) {
                continue;
            }
            if (preferredProviderId.equals(providerModel.getProviderId())) {
                preferred.add(providerModel);
            } else {
                others.add(providerModel);
            }
        }
        if (preferred.isEmpty()) {
            return providerModels;
        }

        List<ProviderModel> ordered = new ArrayList<>(providerModels.size());
        ordered.addAll(preferred);
        ordered.addAll(others);
        return ordered;
    }

    public record ResolvedProviderRoute(Provider provider,
                                        ProviderModel providerModel,
                                        ProviderToken providerToken,
                                        ProviderPermitLease providerPermitLease) {
    }
}
