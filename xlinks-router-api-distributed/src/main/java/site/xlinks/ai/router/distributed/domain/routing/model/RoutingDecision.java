package site.xlinks.ai.router.distributed.domain.routing.model;

import lombok.Builder;
import lombok.Data;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

@Data
@Builder
public class RoutingDecision {

    private Long accountId;

    private Long modelId;

    private Long preferredProviderId;

    private Provider provider;

    private ProviderModel providerModel;

    private ProviderToken providerToken;

    private String decisionStage;
}
