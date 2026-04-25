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
        if (!policy.concurrencyLimitEnabled() || policy.maxConcurrentPerToken() <= 0) {
            ProxyRequestTrace.addRouteEvent("providerToken=" + token.getId() + " 未启用并发限制，直接放行");
            return new ProviderPermitLease(
                    provider.getId(),
                    token.getId(),
                    token.getTokenName(),
                    token.getTokenValue(),
                    null,
                    policy
            );
        }

        String semaphoreKey = buildSemaphoreKey(provider.getId(), token.getId());
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
                        + " 触发并发限流，未拿到许可(maxConcurrent=" + policy.maxConcurrentPerToken()
                        + ", acquireTimeoutMs=" + policy.acquireTimeoutMs() + ")");
                return null;
            }
            ProxyRequestTrace.addRouteEvent("providerToken=" + token.getId()
                    + " 获取并发许可成功(permitId=" + permitId
                    + ", maxConcurrent=" + policy.maxConcurrentPerToken() + ")");
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
            return null;
        }
    }

    public ScheduledFuture<?> scheduleAutoRenew(ProviderInvokeContext context) {
        if (context == null || context.getProviderPermitId() == null || context.getSessionRenewIntervalMs() == null) {
            return null;
        }
        long intervalMs = Math.max(context.getSessionRenewIntervalMs(), 1000);
        ProxyRequestTrace.addRouteEvent("启动 provider token 许可续租任务(intervalMs=" + intervalMs + ")");
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
            ProxyRequestTrace.addRouteEvent("释放 provider token 并发许可成功(permitId=" + context.getProviderPermitId() + ")");
        } catch (Exception e) {
            log.warn("释放 provider token 并发许可失败。providerId={}, providerTokenId={}, requestId={}, permitId={}, msg={}",
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
        } catch (Exception e) {
            log.warn("续租 provider token 许可失败。providerId={}, providerTokenId={}, requestId={}, permitId={}, msg={}",
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
                return;
            }
            if (currentConfigured == maxPermits) {
                return;
            }
            semaphore.addPermits(maxPermits - currentConfigured);
            configuredBucket.set(maxPermits);
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
