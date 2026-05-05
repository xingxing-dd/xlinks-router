package site.xlinks.ai.router.app.forwarding.retry;

import org.springframework.stereotype.Component;
import site.xlinks.ai.router.infrastructure.http.exception.UpstreamRetryableException;

@Component
public class DefaultForwardFailureRoutingStrategy implements ForwardFailureRoutingStrategy {

    @Override
    public ForwardFailureAction decide(Throwable failure, int attempt, int maxAttempts) {
        if (failure instanceof UpstreamRetryableException && attempt < maxAttempts) {
            return ForwardFailureAction.SWITCH_PROVIDER;
        }
        return ForwardFailureAction.FAIL_FAST;
    }
}
