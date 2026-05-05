package site.xlinks.ai.router.distributed.domain.routing.model;

import lombok.Builder;
import lombok.Data;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingConsumptionMode;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ProviderModel;

import java.util.List;

/**
 * 路由决策阶段产出的执行计划。
 * 这里负责沉淀鉴权、消费方式、模型以及候选服务商等决策结果，
 * 不负责实际选择 provider token 或获取 permit。
 */
@Data
@Builder
public class RoutingPlan {

    private CustomerToken customerToken;

    private CustomerAccount customerAccount;

    private CustomerPlan customerPlan;

    private Model model;

    private ForwardingConsumptionMode consumptionMode;

    private Long accountId;

    private Long modelId;

    private Long preferredProviderId;

    private List<ProviderModel> candidateProviderModels;

    private List<ProviderModel> orderedProviderModels;

    private RoutingDecisionStage decisionStage;
}
