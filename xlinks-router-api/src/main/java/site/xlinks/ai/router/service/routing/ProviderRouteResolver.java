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
import site.xlinks.ai.router.service.ProxyRequestTrace;
import site.xlinks.ai.router.service.RouteCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        return resolve(accountId, modelId, modelCode, protocol, requestId, Set.of(), Set.of());
    }

    public ResolvedProviderRoute resolve(Long accountId,
                                         Long modelId,
                                         String modelCode,
                                         ProxyProtocol protocol,
                                         String requestId,
                                         Set<Long> excludedProviderIds,
                                         Set<Long> excludedProviderTokenIds) {
        List<ProviderModel> providerModels = routeCacheService.listProviderModelsByPriority(modelId, protocol);
        if (providerModels == null || providerModels.isEmpty()) {
            throw ProxyErrors.noProviderMapping(modelCode);
        }

        providerModels = prioritizeMerchantConfiguredProvider(accountId, modelId, providerModels);
        boolean concurrencyLimited = false;
        Long preferredProviderId = routeCacheService.getMerchantPreferredProviderId(accountId, modelId);
        ProxyRequestTrace.addRouteEvent("Start resolving upstream provider, candidateCount=" + providerModels.size()
                + ", preferredProviderId=" + preferredProviderId);
        log.info("Provider route resolve start. requestId={}, accountId={}, modelId={}, modelCode={}, protocol={}, candidateCount={}, preferredProviderId={}, excludedProviderIds={}, excludedProviderTokenIds={}",
                requestId,
                accountId,
                modelId,
                modelCode,
                protocol,
                providerModels.size(),
                preferredProviderId,
                excludedProviderIds,
                excludedProviderTokenIds);

        for (ProviderModel candidate : providerModels) {
            if (candidate == null || candidate.getProviderId() == null) {
                continue;
            }
            log.info("Provider route candidate evaluating. requestId={}, providerId={}, providerModelId={}, providerModelName={}",
                    requestId,
                    candidate.getProviderId(),
                    candidate.getId(),
                    candidate.getProviderModelName());
            if (excludedProviderIds != null && excludedProviderIds.contains(candidate.getProviderId())) {
                ProxyRequestTrace.addRouteEvent("Skip excluded provider(providerId=" + candidate.getProviderId() + ")");
                log.info("Provider route candidate skipped because provider is excluded. requestId={}, providerId={}, providerModelId={}",
                        requestId,
                        candidate.getProviderId(),
                        candidate.getId());
                continue;
            }
            if (routeCacheService.isProviderTemporarilyUnavailable(candidate.getProviderId())) {
                ProxyRequestTrace.addRouteEvent("Skip temporarily unavailable provider(providerId=" + candidate.getProviderId() + ")");
                log.info("Provider route candidate skipped because provider is temporarily unavailable. requestId={}, providerId={}, providerModelId={}",
                        requestId,
                        candidate.getProviderId(),
                        candidate.getId());
                continue;
            }
            Provider candidateProvider = routeCacheService.getProvider(candidate.getProviderId());
            if (candidateProvider == null || candidateProvider.getStatus() == null || candidateProvider.getStatus() != 1) {
                ProxyRequestTrace.addRouteEvent("Skip unavailable provider(providerId=" + candidate.getProviderId()
                        + ", providerStatus=" + (candidateProvider == null ? null : candidateProvider.getStatus()) + ")");
                log.info("Provider route candidate skipped because provider status is unavailable. requestId={}, providerId={}, providerModelId={}, providerStatus={}",
                        requestId,
                        candidate.getProviderId(),
                        candidate.getId(),
                        candidateProvider == null ? null : candidateProvider.getStatus());
                continue;
            }
            ProviderTokenSelectService.SelectionResult selectionResult =
                    providerTokenSelectService.selectTokenLeaseOrNull(
                            candidateProvider,
                            requestId,
                            excludedProviderTokenIds
                    );
            if (selectionResult.token() == null) {
                concurrencyLimited = concurrencyLimited || selectionResult.concurrencyLimited();
                ProxyRequestTrace.addRouteEvent("Provider did not select available token, continue next provider(providerId="
                        + candidateProvider.getId() + ", concurrencyLimited=" + selectionResult.concurrencyLimited() + ")");
                log.info("Provider route candidate token selection missed. requestId={}, providerId={}, providerCode={}, providerModelId={}, concurrencyLimited={}",
                        requestId,
                        candidateProvider.getId(),
                        candidateProvider.getProviderCode(),
                        candidate.getId(),
                        selectionResult.concurrencyLimited());
                continue;
            }
            ProxyRequestTrace.addRouteEvent("Upstream provider selected(providerId=" + candidateProvider.getId()
                    + ", providerName=" + candidateProvider.getProviderName()
                    + ", providerModelId=" + candidate.getId()
                    + ", providerModelName=" + candidate.getProviderModelName()
                    + ", providerTokenId=" + selectionResult.token().getId() + ")");
            log.info("Provider route resolve success. requestId={}, providerId={}, providerCode={}, providerModelId={}, providerModelName={}, providerTokenId={}, permitAllocated={}",
                    requestId,
                    candidateProvider.getId(),
                    candidateProvider.getProviderCode(),
                    candidate.getId(),
                    candidate.getProviderModelName(),
                    selectionResult.token().getId(),
                    selectionResult.lease() != null && selectionResult.lease().hasPermit());
            return new ResolvedProviderRoute(
                    candidateProvider,
                    candidate,
                    selectionResult.token(),
                    selectionResult.lease()
            );
        }

        if (concurrencyLimited) {
            ProxyRequestTrace.addRouteEvent("All candidate providers missed because of concurrency limit");
            log.info("Provider route resolve finished with concurrency limited. requestId={}, accountId={}, modelId={}, modelCode={}, protocol={}",
                    requestId,
                    accountId,
                    modelId,
                    modelCode,
                    protocol);
            throw ProxyErrors.providerTokenRateLimited();
        }
        ProxyRequestTrace.addRouteEvent("All candidate providers have no available token");
        log.info("Provider route resolve finished without available token. requestId={}, accountId={}, modelId={}, modelCode={}, protocol={}",
                requestId,
                accountId,
                modelId,
                modelCode,
                protocol);
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
