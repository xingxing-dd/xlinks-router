package site.xlinks.ai.router.distributed.infrastructure.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import site.xlinks.ai.router.distributed.app.forwarding.model.ProviderPermitLease;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderConcurrencyGuardTest {

    private RedissonClient redissonClient;
    private ThreadPoolTaskScheduler renewScheduler;
    private ProviderConcurrencyGuard providerConcurrencyGuard;
    private RLock lock;
    private RBucket<Integer> configuredBucket;
    private RPermitExpirableSemaphore semaphore;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        renewScheduler = mock(ThreadPoolTaskScheduler.class);
        providerConcurrencyGuard = new ProviderConcurrencyGuard(redissonClient, renewScheduler);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultMaxConcurrentPerToken", 0);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultAcquireTimeoutMs", 500);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultRequestTimeoutMs", 40000);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultStreamFirstResponseTimeoutMs", 40000);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultStreamIdleTimeoutMs", 30000);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultSessionLeaseMs", 30000);
        ReflectionTestUtils.setField(providerConcurrencyGuard, "defaultSessionRenewIntervalMs", 15000);

        lock = mock(RLock.class);
        configuredBucket = mock(RBucket.class);
        semaphore = mock(RPermitExpirableSemaphore.class);

        String semaphoreKey = "xlinks:router:provider:1001:token:2001:permits";
        when(redissonClient.getLock(semaphoreKey + ":config:lock")).thenReturn(lock);
        when(redissonClient.<Integer>getBucket(semaphoreKey + ":config:max")).thenReturn(configuredBucket);
        when(redissonClient.getPermitExpirableSemaphore(semaphoreKey)).thenReturn(semaphore);
    }

    @Test
    void shouldReconcileSemaphoreWhenConfigBucketMissingButExistingPermitsRemain() throws Exception {
        Provider provider = provider();
        ProviderToken token = token();

        when(semaphore.availablePermits()).thenReturn(0, 1);
        when(semaphore.acquiredPermits()).thenReturn(0, 0);
        when(semaphore.trySetPermits(2)).thenReturn(false);
        when(semaphore.tryAcquire(eq(500L), eq(30000L), eq(TimeUnit.MILLISECONDS))).thenReturn("permit-1");

        ProviderPermitLease lease = providerConcurrencyGuard.tryAcquire(provider, token, "req-1");

        assertNotNull(lease);
        verify(semaphore).trySetPermits(2);
        verify(semaphore).addPermits(1);
        verify(configuredBucket).set(2);
        verify(semaphore, never()).setPermits(2);
    }

    private Provider provider() {
        Provider provider = new Provider();
        provider.setId(1001L);
        provider.setProviderCode("provider-1001");
        provider.setProviderName("provider-1001");
        provider.setConcurrencyLimitEnabled(1);
        provider.setMaxConcurrentPerToken(2);
        provider.setAcquireTimeoutMs(500);
        provider.setRequestTimeoutMs(40000);
        provider.setStreamFirstResponseTimeoutMs(40000);
        provider.setStreamIdleTimeoutMs(30000);
        provider.setSessionLeaseMs(30000);
        provider.setSessionRenewIntervalMs(15000);
        return provider;
    }

    private ProviderToken token() {
        ProviderToken token = new ProviderToken();
        token.setId(2001L);
        token.setTokenName("token-2001");
        return token;
    }
}
