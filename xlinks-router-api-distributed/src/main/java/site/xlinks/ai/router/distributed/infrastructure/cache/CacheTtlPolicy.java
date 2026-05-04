package site.xlinks.ai.router.distributed.infrastructure.cache;

import java.time.Duration;

public final class CacheTtlPolicy {

    private CacheTtlPolicy() {
    }

    public static final Duration ROUTING_READ_MODEL_TTL = Duration.ofMinutes(10);
    public static final Duration CUSTOMER_TOKEN_TTL = Duration.ofMinutes(10);
    public static final Duration PROVIDER_RUNTIME_STATE_TTL = Duration.ofMinutes(10);
    public static final Duration VERSION_TTL = Duration.ofHours(1);
}
