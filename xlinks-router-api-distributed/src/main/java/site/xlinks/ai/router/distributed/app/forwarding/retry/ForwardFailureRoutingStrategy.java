package site.xlinks.ai.router.distributed.app.forwarding.retry;

public interface ForwardFailureRoutingStrategy {

    ForwardFailureAction decide(Throwable failure, int attempt, int maxAttempts);
}
