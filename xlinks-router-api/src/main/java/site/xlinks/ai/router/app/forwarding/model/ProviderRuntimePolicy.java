package site.xlinks.ai.router.app.forwarding.model;

public record ProviderRuntimePolicy(boolean concurrencyLimitEnabled,
                                    int maxConcurrentPerToken,
                                    int acquireTimeoutMs,
                                    int requestTimeoutMs,
                                    int streamFirstResponseTimeoutMs,
                                    int streamIdleTimeoutMs,
                                    int sessionLeaseMs,
                                    int sessionRenewIntervalMs) {
}
