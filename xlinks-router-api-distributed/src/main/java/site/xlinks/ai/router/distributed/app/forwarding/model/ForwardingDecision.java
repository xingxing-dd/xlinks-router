package site.xlinks.ai.router.distributed.app.forwarding.model;

import lombok.Builder;
import lombok.Data;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingPlan;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;

/**
 * 转发决策结果。
 * 由决策阶段产出，再交给执行阶段完成 provider/token 选择、permit 控制和网络转发。
 */
@Data
@Builder
public class ForwardingDecision {

    private ForwardRequest request;

    private CustomerToken customerToken;

    private CustomerAccount customerAccount;

    private CustomerPlan customerPlan;

    private Model model;

    private String requestId;

    private ForwardingConsumptionMode consumptionMode;

    private RoutingPlan routingPlan;

    /**
     * 当前执行尝试实际选中的 provider/providerModel/providerToken。
     */
    private RoutingDecision selectedRoute;

    private ProviderPermitLease providerPermitLease;

    /**
     * 记录会话级 permit 真正持有的起始时间。
     */
    private Long sessionStartedAtMs;

    private ForwardingUsageContext usageContext;

    private ForwardingStage stage;

    private ForwardingNextAction nextAction;
}
