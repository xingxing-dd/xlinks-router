package site.xlinks.ai.router.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        var singleServer = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase());
        if (StringUtils.hasText(redisProperties.getPassword())) {
            singleServer.setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean("providerPermitRenewScheduler")
    public ThreadPoolTaskScheduler providerPermitRenewScheduler(
            @Value("${xlinks.router.async.permit-renew.pool-size:4}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(poolSize, 1));
        scheduler.setThreadNamePrefix("provider-permit-renew-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
