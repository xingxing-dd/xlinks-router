package site.xlinks.ai.router.infrastructure.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.app.forwarding.model.ProviderPermitLease;
import site.xlinks.ai.router.app.forwarding.model.ProviderRuntimePolicy;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector.RequestChainLogSession;
import site.xlinks.ai.router.support.logging.RequestChainLogType;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
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

    /**
     * 获取会话级并发令牌。非流式场景持有到请求结束，流式场景持有到最后一个数据包写完。
     */
    public ProviderPermitLease tryAcquire(Provider provider, ProviderToken token, String requestId) {
        if (provider == null || token == null || token.getId() == null) {
            return null;
        }

        ProviderRuntimePolicy policy = resolvePolicy(provider);
        String semaphoreKey = buildSemaphoreKey(provider.getId(), token.getId());
        if (!policy.concurrencyLimitEnabled() || policy.maxConcurrentPerToken() <= 0) {
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_BYPASSED,
                    describeProvider(provider),
                    describeProviderToken(token)
            );
            return new ProviderPermitLease(
                    provider.getId(),
                    token.getId(),
                    token.getTokenName(),
                    null,
                    policy
            );
        }

        ensureSemaphoreConfigured(semaphoreKey, policy.maxConcurrentPerToken());
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(semaphoreKey);

        try {
            recordPermitDiagnostic("获取前", provider, token, semaphoreKey, semaphore, null);
            String permitId = semaphore.tryAcquire(
                    Math.max(policy.acquireTimeoutMs(), 0),
                    Math.max(policy.sessionLeaseMs(), 1),
                    TimeUnit.MILLISECONDS
            );
            if (permitId == null || permitId.isBlank()) {
                recordPermitDiagnostic("获取失败后", provider, token, semaphoreKey, semaphore, null);
                return null;
            }
            recordPermitDiagnostic("获取成功后", provider, token, semaphoreKey, semaphore, permitId);
            return new ProviderPermitLease(
                    provider.getId(),
                    token.getId(),
                    token.getTokenName(),
                    permitId,
                    policy
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_ACQUIRE_INTERRUPTED,
                    describeProvider(provider),
                    describeProviderToken(token)
            );
            return null;
        }
    }

    public ScheduledFuture<?> scheduleAutoRenew(String requestId,
                                                Provider provider,
                                                ProviderToken token,
                                                ProviderPermitLease permitLease) {
        if (permitLease == null || permitLease.permitId() == null || permitLease.runtimePolicy() == null) {
            return null;
        }
        long intervalMs = Math.max(permitLease.runtimePolicy().sessionRenewIntervalMs(), 1000);
        RequestChainLogSession logSession = RequestChainLogCollector.captureCurrent();
        try {
            ScheduledFuture<?> future = renewScheduler.scheduleAtFixedRate(
                    () -> {
                        RequestChainLogCollector.bind(logSession);
                        try {
                            renewQuietly(requestId, provider, token, permitLease);
                        } finally {
                            RequestChainLogCollector.clear();
                        }
                    },
                    Duration.ofMillis(intervalMs)
            );
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_RENEW_SCHEDULED,
                    describeProvider(provider),
                    describeProviderToken(token),
                    intervalMs
            );
            return future;
        } catch (TaskRejectedException ex) {
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_RENEW_FAILED,
                    describeProvider(provider),
                    describeProviderToken(token),
                    permitLease.permitId(),
                    valueOrDash(ex.getMessage())
            );
            log.warn("自动续约任务未能启动。providerId={}, providerTokenId={}, permitId={}, reason={}",
                    provider == null ? null : provider.getId(),
                    token == null ? null : token.getId(),
                    permitLease.permitId(),
                    ex.getMessage());
            return null;
        }
    }

    public void cancelAutoRenew(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    public void releaseQuietly(String requestId,
                               Provider provider,
                               ProviderToken token,
                               ProviderPermitLease permitLease) {
        if (permitLease == null || permitLease.permitId() == null
                || provider == null || provider.getId() == null
                || token == null || token.getId() == null) {
            return;
        }
        String semaphoreKey = buildSemaphoreKey(provider.getId(), token.getId());
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(semaphoreKey);
        try {
            recordPermitDiagnostic("释放前", provider, token, semaphoreKey, semaphore, permitLease.permitId());
            semaphore.release(permitLease.permitId());
            recordPermitDiagnostic("释放后", provider, token, semaphoreKey, semaphore, permitLease.permitId());
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_RELEASED,
                    describeProvider(provider),
                    describeProviderToken(token),
                    permitLease.permitId()
            );
        } catch (Exception ex) {
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_RELEASE_FAILED,
                    describeProvider(provider),
                    describeProviderToken(token),
                    permitLease.permitId(),
                    valueOrDash(ex.getMessage())
            );
        }
    }

    private void renewQuietly(String requestId,
                              Provider provider,
                              ProviderToken token,
                              ProviderPermitLease permitLease) {
        try {
            redissonClient.getPermitExpirableSemaphore(buildSemaphoreKey(provider.getId(), token.getId()))
                    .updateLeaseTime(
                            permitLease.permitId(),
                            Math.max(permitLease.runtimePolicy().sessionLeaseMs(), 1),
                            TimeUnit.MILLISECONDS
                    );
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_RENEWED,
                    describeProvider(provider),
                    describeProviderToken(token),
                    permitLease.permitId()
            );
        } catch (Exception ex) {
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_RENEW_FAILED,
                    describeProvider(provider),
                    describeProviderToken(token),
                    permitLease.permitId(),
                    valueOrDash(ex.getMessage())
            );
        }
    }

    private void ensureSemaphoreConfigured(String semaphoreKey, int maxPermits) {
        RLock lock = redissonClient.getLock(semaphoreKey + ":config:lock");
        lock.lock();
        try {
            RBucket<Integer> configuredBucket = redissonClient.getBucket(semaphoreKey + ":config:max");
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(semaphoreKey);
            int actualConfigured = resolveActualConfiguredPermits(semaphore, maxPermits);
            if (actualConfigured == maxPermits) {
                configuredBucket.set(maxPermits);
                return;
            }

            if (actualConfigured < maxPermits) {
                semaphore.addPermits(maxPermits - actualConfigured);
                configuredBucket.set(maxPermits);
                return;
            }

            int acquiredPermits = Math.max(semaphore.acquiredPermits(), 0);
            if (acquiredPermits == 0) {
                semaphore.setPermits(maxPermits);
                configuredBucket.set(maxPermits);
                return;
            }

            configuredBucket.set(actualConfigured);
            log.warn("服务商并发令牌总量大于目标值，且当前仍有会话占用，暂不收缩。semaphoreKey={}, targetPermits={}, actualPermits={}, acquiredPermits={}",
                    semaphoreKey, maxPermits, actualConfigured, acquiredPermits);
        } catch (Exception ex) {
            log.warn("校准服务商并发令牌配置失败。semaphoreKey={}, targetPermits={}", semaphoreKey, maxPermits, ex);
            throw ex;
        } finally {
            lock.unlock();
        }
    }

    private void recordPermitDiagnostic(String action,
                                        Provider provider,
                                        ProviderToken token,
                                        String semaphoreKey,
                                        RPermitExpirableSemaphore semaphore,
                                        String permitId) {
        if (semaphore == null) {
            return;
        }
        try {
            RequestChainLogCollector.record(
                    RequestChainLogType.PERMIT_DIAGNOSTIC,
                    action,
                    describeProvider(provider),
                    describeProviderToken(token),
                    semaphoreKey,
                    semaphore.availablePermits(),
                    semaphore.acquiredPermits(),
                    valueOrDash(permitId)
            );
        } catch (Exception ex) {
            log.warn("记录并发令牌诊断信息失败。semaphoreKey={}, action={}", semaphoreKey, action, ex);
        }
    }

    private int resolveActualConfiguredPermits(RPermitExpirableSemaphore semaphore, int maxPermits) {
        if (semaphore == null) {
            return 0;
        }
        int availablePermits = Math.max(semaphore.availablePermits(), 0);
        int acquiredPermits = Math.max(semaphore.acquiredPermits(), 0);
        int actualConfigured = availablePermits + acquiredPermits;
        if (actualConfigured > 0) {
            return actualConfigured;
        }

        boolean initialized = semaphore.trySetPermits(maxPermits);
        if (initialized) {
            return maxPermits;
        }

        availablePermits = Math.max(semaphore.availablePermits(), 0);
        acquiredPermits = Math.max(semaphore.acquiredPermits(), 0);
        actualConfigured = availablePermits + acquiredPermits;
        if (actualConfigured > 0) {
            return actualConfigured;
        }

        semaphore.setPermits(maxPermits);
        return maxPermits;
    }

    private ProviderRuntimePolicy resolvePolicy(Provider provider) {
        boolean concurrencyEnabled = provider != null
                && provider.getConcurrencyLimitEnabled() != null
                && provider.getConcurrencyLimitEnabled() == 1;
        int maxConcurrent = normalized(provider == null ? null : provider.getMaxConcurrentPerToken(), defaultMaxConcurrentPerToken);
        return new ProviderRuntimePolicy(
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

    private String describeProvider(Provider provider) {
        if (provider == null) {
            return "-";
        }
        return valueOrDash(provider.getProviderName())
                + "/" + valueOrDash(provider.getProviderCode())
                + "(" + valueOrDash(provider.getId()) + ")";
    }

    private String describeProviderToken(ProviderToken token) {
        if (token == null) {
            return "-";
        }
        return valueOrDash(token.getTokenName()) + "(" + valueOrDash(token.getId()) + ")";
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        return text.isBlank() ? "-" : text;
    }
}
