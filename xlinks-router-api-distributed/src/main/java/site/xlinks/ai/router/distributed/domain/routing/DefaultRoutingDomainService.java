package site.xlinks.ai.router.distributed.domain.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.domain.provider.ProviderTokenSelectionService;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingContext;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.distributed.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultRoutingDomainService implements RoutingDomainService {

    private final DistributedRouteCacheRepository distributedRouteCacheRepository;
    private final ForwardingReadModelLoader forwardingReadModelLoader;
    private final ProviderTokenSelectionService providerTokenSelectionService;

    @Override
    public RoutingDecision route(RoutingContext context) {
        Long accountId = context.getCustomerAccount() == null ? null : context.getCustomerAccount().getId();
        Long modelId = context.getModel() == null ? null : context.getModel().getId();
        if (accountId == null || modelId == null || context.getRequest() == null || context.getRequest().getProtocol() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Routing context is incomplete");
        }

        validateAllowedModels(context);

        List<ProviderModel> candidates = forwardingReadModelLoader.loadRoutingIndex(modelId, context.getRequest().getProtocol());
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.PROVIDER_UNAVAILABLE, "No provider route available");
        }

        Long preferredProviderId = forwardingReadModelLoader.loadMerchantPreferredProvider(accountId, modelId);
        List<ProviderModel> ordered = prioritizePreferredProvider(candidates, preferredProviderId);

        for (ProviderModel providerModel : ordered) {
            if (providerModel == null || providerModel.getProviderId() == null) {
                continue;
            }
            if (isProviderTemporarilyUnavailable(providerModel.getProviderId())) {
                continue;
            }
            Provider provider = forwardingReadModelLoader.loadProvider(providerModel.getProviderId());
            if (provider == null || provider.getStatus() == null || provider.getStatus() != 1) {
                continue;
            }
            List<ProviderToken> providerTokens = forwardingReadModelLoader.loadProviderTokens(provider.getId());
            ProviderToken providerToken = providerTokenSelectionService.select(provider, providerTokens);
            if (providerToken == null) {
                continue;
            }
            return RoutingDecision.builder()
                    .accountId(accountId)
                    .modelId(modelId)
                    .preferredProviderId(preferredProviderId)
                    .provider(provider)
                    .providerModel(providerModel)
                    .providerToken(providerToken)
                    .decisionStage("ROUTED")
                    .build();
        }

        throw new BusinessException(ErrorCode.PROVIDER_TOKEN_UNAVAILABLE, "No provider token available");
    }

    private List<ProviderModel> prioritizePreferredProvider(List<ProviderModel> candidates, Long preferredProviderId) {
        if (preferredProviderId == null || candidates == null || candidates.isEmpty()) {
            return candidates;
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
            return candidates;
        }
        List<ProviderModel> ordered = new ArrayList<>(candidates.size());
        ordered.addAll(preferred);
        ordered.addAll(others);
        return ordered;
    }

    private void validateAllowedModels(RoutingContext context) {
        String modelCode = context.getModel().getModelCode();
        AllowedModelsRule customerRule = forwardingReadModelLoader.loadCustomerAllowedModelsRule(context.getCustomerToken());
        if (!matches(customerRule, modelCode)) {
            throw new BusinessException(ErrorCode.MODEL_NOT_IN_ALLOWED_LIST, "Customer token does not allow current model");
        }
        AllowedModelsRule planRule = forwardingReadModelLoader.loadPlanAllowedModelsRule(context.getCustomerPlan());
        if (!matches(planRule, modelCode)) {
            throw new BusinessException(ErrorCode.MODEL_NOT_IN_ALLOWED_LIST, "Customer plan does not allow current model");
        }
    }

    private boolean matches(AllowedModelsRule rule, String modelCode) {
        if (rule == null || rule.isAllowAll()) {
            return true;
        }
        if (modelCode == null || modelCode.isBlank()) {
            return false;
        }
        return rule.getAllowedModels() == null
                || rule.getAllowedModels().isEmpty()
                || rule.getAllowedModels().contains(modelCode.trim());
    }

    private boolean isProviderTemporarilyUnavailable(Long providerId) {
        ProviderFailureState state = distributedRouteCacheRepository.getProviderFailureState(providerId);
        return state != null && state.getFailureCount() > 0;
    }
}
