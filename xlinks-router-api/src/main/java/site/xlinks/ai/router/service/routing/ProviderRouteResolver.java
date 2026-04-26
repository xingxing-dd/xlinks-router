package site.xlinks.ai.router.service.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.service.ProviderPermitLease;
import site.xlinks.ai.router.service.ProxyRequestTrace;
import site.xlinks.ai.router.service.ProviderTokenSelectService;
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
        ProxyRequestTrace.addRouteEvent("开始选择上游 provider，候选数=" + providerModels.size()
                + ", preferredProviderId=" + preferredProviderId);

        for (ProviderModel candidate : providerModels) {
            if (candidate == null || candidate.getProviderId() == null) {
                continue;
            }
            if (excludedProviderIds != null && excludedProviderIds.contains(candidate.getProviderId())) {
                ProxyRequestTrace.addRouteEvent("跳过已重试失败 provider(providerId="
                        + candidate.getProviderId() + ")");
                continue;
            }
            if (routeCacheService.isProviderTemporarilyUnavailable(candidate.getProviderId())) {
                ProxyRequestTrace.addRouteEvent("触发降级，跳过临时不可用 provider(providerId="
                        + candidate.getProviderId() + ")");
                continue;
            }
            Provider candidateProvider = routeCacheService.getProvider(candidate.getProviderId());
            if (candidateProvider == null || candidateProvider.getStatus() == null || candidateProvider.getStatus() != 1) {
                ProxyRequestTrace.addRouteEvent("跳过不可用 provider(providerId=" + candidate.getProviderId()
                        + ", providerStatus=" + (candidateProvider == null ? null : candidateProvider.getStatus()) + ")");
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
                ProxyRequestTrace.addRouteEvent("provider=" + candidateProvider.getId()
                        + " 未选到可用 token，继续尝试下一个 provider(concurrencyLimited="
                        + selectionResult.concurrencyLimited() + ")");
                continue;
            }
            ProxyRequestTrace.addRouteEvent("上游 provider 选择成功(providerId=" + candidateProvider.getId()
                    + ", providerName=" + candidateProvider.getProviderName()
                    + ", providerModelId=" + candidate.getId()
                    + ", providerModelName=" + candidate.getProviderModelName()
                    + ", providerTokenId=" + selectionResult.token().getId() + ")");
            return new ResolvedProviderRoute(
                    candidateProvider,
                    candidate,
                    selectionResult.token(),
                    selectionResult.lease()
            );
        }

        if (concurrencyLimited) {
            ProxyRequestTrace.addRouteEvent("所有候选 provider 均因限流未命中");
            throw ProxyErrors.providerTokenRateLimited();
        }
        ProxyRequestTrace.addRouteEvent("所有候选 provider 均无可用 token");
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
