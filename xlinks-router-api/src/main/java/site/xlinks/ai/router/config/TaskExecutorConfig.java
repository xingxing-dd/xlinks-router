package site.xlinks.ai.router.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated executors for SSE forwarding and async persistence.
 */
@Configuration
public class TaskExecutorConfig implements AsyncConfigurer {

    @Value("${xlinks.router.async.usage.core-size:4}")
    private int usageCoreSize;

    @Value("${xlinks.router.async.usage.max-size:16}")
    private int usageMaxSize;

    @Value("${xlinks.router.async.usage.queue-capacity:1000}")
    private int usageQueueCapacity;

    @Value("${xlinks.router.async.sse.core-size:8}")
    private int sseCoreSize;

    @Value("${xlinks.router.async.sse.max-size:64}")
    private int sseMaxSize;

    @Value("${xlinks.router.async.sse.queue-capacity:2000}")
    private int sseQueueCapacity;

    @Value("${xlinks.router.async.sse-writer.core-size:8}")
    private int sseWriterCoreSize;

    @Value("${xlinks.router.async.sse-writer.max-size:64}")
    private int sseWriterMaxSize;

    @Value("${xlinks.router.async.sse-writer.queue-capacity:2000}")
    private int sseWriterQueueCapacity;

    @Value("${xlinks.router.async.permit-renew.pool-size:4}")
    private int permitRenewPoolSize;

    @Bean("usageTaskExecutor")
    public ThreadPoolTaskExecutor usageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(usageCoreSize);
        executor.setMaxPoolSize(usageMaxSize);
        executor.setQueueCapacity(usageQueueCapacity);
        executor.setThreadNamePrefix("usage-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("sseTaskExecutor")
    public TaskExecutor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(sseCoreSize);
        executor.setMaxPoolSize(sseMaxSize);
        executor.setQueueCapacity(sseQueueCapacity);
        executor.setThreadNamePrefix("sse-forward-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("sseWriterTaskExecutor")
    public TaskExecutor sseWriterTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(sseWriterCoreSize);
        executor.setMaxPoolSize(sseWriterMaxSize);
        executor.setQueueCapacity(sseWriterQueueCapacity);
        executor.setThreadNamePrefix("sse-write-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("providerPermitRenewScheduler")
    public ThreadPoolTaskScheduler providerPermitRenewScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(permitRenewPoolSize);
        scheduler.setThreadNamePrefix("permit-renew-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public Executor getAsyncExecutor() {
        return usageTaskExecutor();
    }
}
