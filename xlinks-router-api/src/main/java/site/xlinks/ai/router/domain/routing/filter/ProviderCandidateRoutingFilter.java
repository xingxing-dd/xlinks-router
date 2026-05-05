package site.xlinks.ai.router.domain.routing.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.domain.routing.model.RoutingDecisionStage;
import site.xlinks.ai.router.domain.routing.model.RoutingExclusions;
import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;
import site.xlinks.ai.router.entity.ProviderModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务商候选过滤器。
 * 负责装载当前模型可用的服务商候选集，并按商户偏好做优先级调整。
 */
@Component
@Order(300)
@RequiredArgsConstructor
public class ProviderCandidateRoutingFilter implements RoutingFilter {

    private final ForwardingReadModelLoader forwardingReadModelLoader;

    @Override
    public void filter(RoutingFilterContext context) {
        List<ProviderModel> candidates = forwardingReadModelLoader.loadRoutingIndex(
                context.getModelId(),
                context.getRequest().getProtocol()
        );
        List<ProviderModel> filteredCandidates = filterExcludedProviders(candidates, context.getExclusions());
        if (filteredCandidates.isEmpty()) {
            throw new BusinessException(ErrorCode.PROVIDER_UNAVAILABLE, "没有可用的服务商路由");
        }

        Long preferredProviderId = forwardingReadModelLoader.loadMerchantPreferredProvider(
                context.getAccountId(),
                context.getModelId()
        );

        context.setPreferredProviderId(preferredProviderId);
        context.setCandidateProviderModels(copyAllowingNulls(filteredCandidates));
        context.setOrderedProviderModels(prioritizePreferredProvider(filteredCandidates, preferredProviderId));
        context.setDecisionStage(RoutingDecisionStage.ROUTED);

        RequestChainLogCollector.record(
                RequestChainLogType.PROVIDER_CANDIDATES_FILTERED,
                filteredCandidates.size(),
                preferredProviderId == null ? "-" : preferredProviderId
        );
    }

    private List<ProviderModel> filterExcludedProviders(List<ProviderModel> candidates, RoutingExclusions exclusions) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (exclusions == null || exclusions.getExcludedProviderIds().isEmpty()) {
            return copyAllowingNulls(candidates);
        }
        return candidates.stream()
                .filter(candidate -> candidate == null
                        || candidate.getProviderId() == null
                        || !exclusions.isProviderExcluded(candidate.getProviderId()))
                .toList();
    }

    private List<ProviderModel> prioritizePreferredProvider(List<ProviderModel> candidates, Long preferredProviderId) {
        if (preferredProviderId == null || candidates == null || candidates.isEmpty()) {
            return copyAllowingNulls(candidates);
        }
        List<ProviderModel> preferred = new ArrayList<>();
        List<ProviderModel> others = new ArrayList<>();
        for (ProviderModel candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (preferredProviderId.equals(candidate.getProviderId())) {
                preferred.add(candidate);
            } else {
                others.add(candidate);
            }
        }
        if (preferred.isEmpty()) {
            return List.copyOf(candidates);
        }
        List<ProviderModel> ordered = new ArrayList<>(candidates.size());
        ordered.addAll(preferred);
        ordered.addAll(others);
        return copyAllowingNulls(ordered);
    }

    private List<ProviderModel> copyAllowingNulls(List<ProviderModel> providerModels) {
        if (providerModels == null || providerModels.isEmpty()) {
            return List.of();
        }
        return List.copyOf(providerModels);
    }
}
