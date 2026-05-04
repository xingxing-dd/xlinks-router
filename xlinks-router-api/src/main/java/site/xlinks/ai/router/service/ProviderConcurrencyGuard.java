package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Distributed provider-token concurrency guard backed by Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderConcurrencyGuard {

    private final RedissonClient redissonClient;

    @Qualifier("providerPermitRenewScheduler")
    private final ThreadPoolTaskScheduler renewScheduler;

    @Value("${xlinks.router.limits.default-max-concurrent-per-token:0}")
    private int defaultMaxConcurrentPerToken;

    @Value("${xlinks.router.limits.default-acquire-timeout-ms:0}")
    private int defaultAcquireTimeoutMs;

    @Value("${xlinks.router.limits.default-request-timeout-ms:20000}")
    private int defaultRequestTimeoutMs;

    @Value("${xlinks.router.limits.default-stream-first-response-timeout-ms:20000}")
    private int defaultStreamFirstResponseTimeoutMs;

    @Value("${xlinks.router.limits.default-stream-idle-timeout-ms:20000}")
    private int defaultStreamIdleTimeoutMs;

    @Value("${xlinks.router.limits.default-session-lease-ms:30000}")
    private int defaultSessionLeaseMs;

    @Value("${xlinks.router.limits.default-session-renew-interval-ms:10000}")
    private int defaultSessionRenewIntervalMs;

    public ProviderPermitLease tryAcquire(Provider provider, ProviderToken token, String requestId) {
        if (provider == null || token == null || token.getId() == null) {
            return null;
        }

        ProxyRuntimePolicy policy = resolvePolicy(provider);
        String semaphoreKey = buildSemaphoreKey(provider.getId(), token.getId());
        log.info("Permit acquire start. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}, concurrencyEnabled={}, maxConcurrentPerToken={}, acquireTimeoutMs={}, sessionLeaseMs={}, sessionRenewIntervalMs={}",
                requestId,
                provider.getId(),
                provider.getProviderCode(),
                token.getId(),
                token.getTokenName(),
                semaphoreKey,
                policy.concurrencyLimitEnabled(),
                policy.maxConcurrentPerToken(),
                policy.acquireTimeoutMs(),
                policy.sessionLeaseMs(),
                policy.sessionRenewIntervalMs());
        if (!policy.concurrencyLimitEnabled() || policy.maxConcurrentPerToken() <= 0) {
            ProxyRequestTrace.addRouteEvent("providerToken=" + token.getId() + " concurrency limit disabled, bypass permit acquire");
            log.info("Permit acquire bypassed because concurrency limit is disabled. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}, concurrencyEnabled={}, maxConcurrentPerToken={}",
                    requestId,
                    provider.getId(),
                    provider.getProviderCode(),
                    token.getId(),
                    token.getTokenName(),
                    semaphoreKey,
                    policy.concurrencyLimitEnabled(),
                    policy.maxConcurrentPerToken());
            return new ProviderPermitLease(
                    provider.getId(),
                    token.getId(),
                    token.getTokenName(),
                    token.getTokenValue(),
                    null,
                    policy
            );
        }

        ensureSemaphoreConfigured(semaphoreKey, policy.maxConcurrentPerToken());

        try {
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(semaphoreKey);
            String permitId = semaphore.tryAcquire(
                    Math.max(policy.acquireTimeoutMs(), 0),
                    Math.max(policy.sessionLeaseMs(), 1),
                    TimeUnit.MILLISECONDS
            );
            if (permitId == null || permitId.isBlank()) {
                ProxyRequestTrace.addRouteEvent("providerToken=" + token.getId()
                        + " concurrency limited, permit not acquired(maxConcurrent=" + policy.maxConcurrentPerToken()
                        + ", acquireTimeoutMs=" + policy.acquireTimeoutMs() + ")");
                log.info("Permit acquire missed. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}, maxConcurrentPerToken={}, acquireTimeoutMs={}, sessionLeaseMs={}",
                        requestId,
                        provider.getId(),
                        provider.getProviderCode(),
                        token.getId(),
                        token.getTokenName(),
                        semaphoreKey,
                        policy.maxConcurrentPerToken(),
                        policy.acquireTimeoutMs(),
                        policy.sessionLeaseMs());
                return null;
            }
            ProxyRequestTrace.addRouteEvent("providerToken=" + token.getId()
                    + " permit acquired successfully(permitId=" + permitId
                    + ", maxConcurrent=" + policy.maxConcurrentPerToken() + ")");
            log.info("Permit acquire success. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}, permitId={}, maxConcurrentPerToken={}, acquireTimeoutMs={}, sessionLeaseMs={}, sessionRenewIntervalMs={}",
                    requestId,
                    provider.getId(),
                    provider.getProviderCode(),
                    token.getId(),
                    token.getTokenName(),
                    semaphoreKey,
                    permitId,
                    policy.maxConcurrentPerToken(),
                    policy.acquireTimeoutMs(),
                    policy.sessionLeaseMs(),
                    policy.sessionRenewIntervalMs());
            return new ProviderPermitLease(
                    provider.getId(),
                    token.getId(),
                    token.getTokenName(),
                    token.getTokenValue(),
                    permitId,
                    policy
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Permit acquire interrupted. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}",
                    requestId,
                    provider.getId(),
                    provider.getProviderCode(),
                    token.getId(),
                    token.getTokenName(),
                    semaphoreKey);
            return null;
        }
    }

    public ScheduledFuture<?> scheduleAutoRenew(ProviderInvokeContext context) {
        if (context == null || context.getProviderPermitId() == null || context.getSessionRenewIntervalMs() == null) {
            return null;
        }
        long intervalMs = Math.max(context.getSessionRenewIntervalMs(), 1000);
        ProxyRequestTrace.addRouteEvent("start provider token permit auto-renew task(intervalMs=" + intervalMs + ")");
        log.info("Permit auto-renew scheduled. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, permitId={}, intervalMs={}, sessionLeaseMs={}",
                context.getRequestId(),
                context.getProviderId(),
                context.getProviderCode(),
                context.getProviderTokenId(),
                context.getProviderTokenName(),
                context.getProviderPermitId(),
                intervalMs,
                context.getSessionLeaseMs());
        return renewScheduler.scheduleAtFixedRate(
                () -> renewQuietly(context),
                Duration.ofMillis(intervalMs)
        );
    }

    public void cancelAutoRenew(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    public void releaseQuietly(ProviderInvokeContext context) {
        if (context == null || context.getProviderPermitId() == null
                || context.getProviderId() == null || context.getProviderTokenId() == null) {
            return;
        }
        try {
            String semaphoreKey = buildSemaphoreKey(context.getProviderId(), context.getProviderTokenId());
            redissonClient.getPermitExpirableSemaphore(semaphoreKey).release(context.getProviderPermitId());
            ProxyRequestTrace.addRouteEvent("release provider token permit success(permitId=" + context.getProviderPermitId() + ")");
            log.info("Permit released. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}, permitId={}",
                    context.getRequestId(),
                    context.getProviderId(),
                    context.getProviderCode(),
                    context.getProviderTokenId(),
                    context.getProviderTokenName(),
                    semaphoreKey,
                    context.getProviderPermitId());
        } catch (Exception e) {
            log.warn("Permit release failed. providerId={}, providerTokenId={}, requestId={}, permitId={}, msg={}",
                    context.getProviderId(),
                    context.getProviderTokenId(),
                    context.getRequestId(),
                    context.getProviderPermitId(),
                    e.getMessage());
        }
    }

    private void renewQuietly(ProviderInvokeContext context) {
        try {
            String semaphoreKey = buildSemaphoreKey(context.getProviderId(), context.getProviderTokenId());
            redissonClient.getPermitExpirableSemaphore(semaphoreKey)
                    .updateLeaseTime(
                            context.getProviderPermitId(),
                            Math.max(context.getSessionLeaseMs(), 1),
                            TimeUnit.MILLISECONDS
                    );
            log.debug("Permit renewed. requestId={}, providerId={}, providerCode={}, providerTokenId={}, tokenName={}, semaphoreKey={}, permitId={}, sessionLeaseMs={}",
                    context.getRequestId(),
                    context.getProviderId(),
                    context.getProviderCode(),
                    context.getProviderTokenId(),
                    context.getProviderTokenName(),
                    semaphoreKey,
                    context.getProviderPermitId(),
                    context.getSessionLeaseMs());
        } catch (Exception e) {
            log.warn("Permit renew failed. providerId={}, providerTokenId={}, requestId={}, permitId={}, msg={}",
                    context.getProviderId(),
                    context.getProviderTokenId(),
                    context.getRequestId(),
                    context.getProviderPermitId(),
                    e.getMessage());
        }
    }

    private void ensureSemaphoreConfigured(String semaphoreKey, int maxPermits) {
        String configLockKey = semaphoreKey + ":config:lock";
        String configValueKey = semaphoreKey + ":config:max";
        RLock lock = redissonClient.getLock(configLockKey);
        lock.lock();
        try {
            RBucket<Integer> configuredBucket = redissonClient.getBucket(configValueKey);
            Integer currentConfigured = configuredBucket.get();
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(semaphoreKey);
            if (currentConfigured == null) {
                semaphore.trySetPermits(maxPermits);
                configuredBucket.set(maxPermits);
                log.info("Semaphore configured first time. semaphoreKey={}, maxPermits={}", semaphoreKey, maxPermits);
                return;
            }
            if (currentConfigured == maxPermits) {
                return;
            }
            semaphore.addPermits(maxPermits - currentConfigured);
            configuredBucket.set(maxPermits);
            log.info("Semaphore permits updated. semaphoreKey={}, previousMaxPermits={}, newMaxPermits={}",
                    semaphoreKey,
                    currentConfigured,
                    maxPermits);
        } finally {
            lock.unlock();
        }
    }

    private ProxyRuntimePolicy resolvePolicy(Provider provider) {
        boolean concurrencyEnabled = provider != null
                && provider.getConcurrencyLimitEnabled() != null
                && provider.getConcurrencyLimitEnabled() == 1;
        int maxConcurrent = normalized(provider == null ? null : provider.getMaxConcurrentPerToken(), defaultMaxConcurrentPerToken);
        return new ProxyRuntimePolicy(
                concurrencyEnabled && maxConcurrent > 0,
                maxConcurrent,
                normalized(provider == null ? null : provider.getAcquireTimeoutMs(), defaultAcquireTimeoutMs),
                normalized(provider == null ? null : provider.getRequestTimeoutMs(), defaultRequestTimeoutMs),
                normalized(provider == null ? null : provider.getStreamFirstResponseTimeoutMs(), defaultStreamFirstResponseTimeoutMs),
                normalized(provider == null ? null : provider.getStreamIdleTimeoutMs(), defaultStreamIdleTimeoutMs),
                normalized(provider == null ? null : provider.getSessionLeaseMs(), defaultSessionLeaseMs),
                normalized(provider == null ? null : provider.getSessionRenewIntervalMs(), defaultSessionRenewIntervalMs)
        );
    }

    private int normalized(Integer value, int fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private String buildSemaphoreKey(Long providerId, Long providerTokenId) {
        return "xlinks:router:provider:" + providerId + ":token:" + providerTokenId + ":permits";
    }
}
