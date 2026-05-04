package site.xlinks.ai.router.distributed.domain.routing.model;

import lombok.Builder;
import lombok.Data;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;

@Data
@Builder
public class RoutingContext {

    private ForwardRequest request;

    private CustomerToken customerToken;

    private CustomerAccount customerAccount;

    private CustomerPlan customerPlan;

    private Model model;
}
