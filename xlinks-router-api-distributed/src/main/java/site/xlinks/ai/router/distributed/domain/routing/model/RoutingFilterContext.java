package site.xlinks.ai.router.distributed.domain.routing.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingConsumptionMode;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ProviderModel;

import java.util.List;

/**
 * 路由过滤链上下文。
 * 过滤器会在这里逐步补齐业务对象，并最终组装为路由计划。
 */
@Data
@RequiredArgsConstructor
public class RoutingFilterContext {

    private final ForwardRequest request;
    private final RoutingExclusions exclusions;

    /**
     * 以下字段由过滤链逐步填充，承载决策阶段需要的业务对象。
     */
    private CustomerToken customerToken;
    private CustomerAccount customerAccount;
    private CustomerPlan customerPlan;
    private Model model;
    private ForwardingConsumptionMode consumptionMode;
    private Long preferredProviderId;
    private List<ProviderModel> candidateProviderModels = List.of();
    private List<ProviderModel> orderedProviderModels = List.of();
    private RoutingDecisionStage decisionStage;

    public static RoutingFilterContext from(ForwardRequest request, RoutingExclusions exclusions) {
        return new RoutingFilterContext(
                request,
                exclusions == null ? RoutingExclusions.none() : exclusions
        );
    }

    public Long getAccountId() {
        return customerAccount == null ? null : customerAccount.getId();
    }

    public Long getModelId() {
        return model == null ? null : model.getId();
    }

    public RoutingPlan toPlan() {
        return RoutingPlan.builder()
                .customerToken(customerToken)
                .customerAccount(customerAccount)
                .customerPlan(customerPlan)
                .model(model)
                .consumptionMode(consumptionMode)
                .accountId(getAccountId())
                .modelId(getModelId())
                .preferredProviderId(preferredProviderId)
                .candidateProviderModels(candidateProviderModels)
                .orderedProviderModels(orderedProviderModels)
                .decisionStage(decisionStage)
                .build();
    }
}
