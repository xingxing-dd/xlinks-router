package site.xlinks.ai.router.distributed.app.forwarding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForwardingApplicationService {

    private final ForwardingDecisionService forwardingDecisionService;
    private final ForwardingExecutionService forwardingExecutionService;

    public Object forward(ForwardRequest request) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        RequestChainLogCollector.bindRequestId(requestId);
        ForwardingDecision decision = forwardingDecisionService.decide(
                request, requestId
        );
        return forwardingExecutionService.execute(decision);
    }
}
