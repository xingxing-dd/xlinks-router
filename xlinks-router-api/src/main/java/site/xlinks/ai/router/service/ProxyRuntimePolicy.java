package site.xlinks.ai.router.service;

/**
 * Resolved runtime policy for one provider invocation.
 */
public record ProxyRuntimePolicy(boolean concurrencyLimitEnabled,
                                 int maxConcurrentPerToken,
                                 int acquireTimeoutMs,
                                 int requestTimeoutMs,
                                 int streamFirstResponseTimeoutMs,
                                 int streamIdleTimeoutMs,
                                 int sessionLeaseMs,
                                 int sessionRenewIntervalMs) {
}
