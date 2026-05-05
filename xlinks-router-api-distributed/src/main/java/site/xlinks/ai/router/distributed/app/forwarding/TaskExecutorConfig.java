package site.xlinks.ai.router.distributed.app.forwarding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class TaskExecutorConfig implements AsyncConfigurer {

    @Value("${xlinks.router.async.usage.core-size:4}")
    private int usageCoreSize;

    @Value("${xlinks.router.async.usage.max-size:16}")
    private int usageMaxSize;

    @Value("${xlinks.router.async.usage.queue-capacity:1000}")
    private int usageQueueCapacity;

    @Value("${xlinks.router.async.sse.core-size:8}")
    private int streamCoreSize;

    @Value("${xlinks.router.async.sse.max-size:64}")
    private int streamMaxSize;

    @Value("${xlinks.router.async.sse.queue-capacity:2000}")
    private int streamQueueCapacity;

    @Bean("usageTaskExecutor")
    public TaskExecutor usageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(usageCoreSize);
        executor.setMaxPoolSize(usageMaxSize);
        executor.setQueueCapacity(usageQueueCapacity);
        executor.setThreadNamePrefix("usage-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 该线程池只负责阻塞读取上游流，客户端写出仍由 Servlet 非阻塞回调驱动。
     */
    @Bean("forwardingStreamTaskExecutor")
    public TaskExecutor forwardingStreamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(streamCoreSize);
        executor.setMaxPoolSize(streamMaxSize);
        executor.setQueueCapacity(streamQueueCapacity);
        executor.setThreadNamePrefix("forwarding-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return usageTaskExecutor();
    }
}
