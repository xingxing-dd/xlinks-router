package site.xlinks.ai.router.distributed.infrastructure.config;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Value("${xlinks.router.http.connect-timeout-ms:10000}")
    private long connectTimeoutMs;

    @Value("${xlinks.router.http.read-timeout-ms:120000}")
    private long readTimeoutMs;

    @Value("${xlinks.router.http.write-timeout-ms:120000}")
    private long writeTimeoutMs;

    @Bean
    public OkHttpClient okHttpClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(512);
        dispatcher.setMaxRequestsPerHost(128);

        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(64, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
    }
}
