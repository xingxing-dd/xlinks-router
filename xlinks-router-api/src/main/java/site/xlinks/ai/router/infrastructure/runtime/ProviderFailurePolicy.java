package site.xlinks.ai.router.infrastructure.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderFailureState;

import java.time.Duration;
import java.time.Instant;

@Component
public class ProviderFailurePolicy {

    private final int tokenConsecutiveFailureThreshold;
    private final Duration tokenBlockedDuration;
    private final Duration tokenFailureStateTtl;

    public ProviderFailurePolicy(
            @Value("${xlinks.router.forward.failure.token-consecutive-failure-threshold:5}") int tokenConsecutiveFailureThreshold,
            @Value("${xlinks.router.forward.failure.token-block-duration-ms:300000}") long tokenBlockDurationMs,
            @Value("${xlinks.router.forward.failure.token-failure-state-ttl-ms:1800000}") long tokenFailureStateTtlMs) {
        this.tokenConsecutiveFailureThreshold = Math.max(tokenConsecutiveFailureThreshold, 1);
        long normalizedBlockDurationMs = Math.max(tokenBlockDurationMs, 1000L);
        long normalizedFailureStateTtlMs = Math.max(tokenFailureStateTtlMs, normalizedBlockDurationMs);
        this.tokenBlockedDuration = Duration.ofMillis(normalizedBlockDurationMs);
        this.tokenFailureStateTtl = Duration.ofMillis(normalizedFailureStateTtlMs);
    }

    public boolean isTemporarilyUnavailable(ProviderFailureState state) {
        return state != null
                && state.getBlockedUntil() != null
                && state.getBlockedUntil().isAfter(Instant.now());
    }

    public int tokenConsecutiveFailureThreshold() {
        return tokenConsecutiveFailureThreshold;
    }

    public Duration tokenBlockedDuration() {
        return tokenBlockedDuration;
    }

    public Duration tokenFailureStateTtl() {
        return tokenFailureStateTtl;
    }
}
