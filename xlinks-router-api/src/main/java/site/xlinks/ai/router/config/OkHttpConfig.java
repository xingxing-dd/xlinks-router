package site.xlinks.ai.router.config;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp client configuration.
 */
@Configuration
public class OkHttpConfig {

    @Value("${xlinks.router.okhttp.connect-timeout-seconds:10}")
    private long connectTimeoutSeconds;

    @Value("${xlinks.router.okhttp.read-timeout-seconds:120}")
    private long readTimeoutSeconds;

    @Value("${xlinks.router.okhttp.write-timeout-seconds:120}")
    private long writeTimeoutSeconds;

    @Value("${xlinks.router.okhttp.max-idle-connections:64}")
    private int maxIdleConnections;

    @Value("${xlinks.router.okhttp.keep-alive-minutes:5}")
    private long keepAliveMinutes;

    @Value("${xlinks.router.okhttp.max-requests:512}")
    private int maxRequests;

    @Value("${xlinks.router.okhttp.max-requests-per-host:128}")
    private int maxRequestsPerHost;

    @Bean
    public OkHttpClient okHttpClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);

        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveMinutes, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
    }
}
