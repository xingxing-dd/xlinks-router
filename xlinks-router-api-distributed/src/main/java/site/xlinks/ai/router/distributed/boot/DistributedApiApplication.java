package site.xlinks.ai.router.distributed.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 分布式 API 转发服务启动入口。
 */
@SpringBootApplication(scanBasePackages = "site.xlinks.ai.router")
@EnableScheduling
@EnableAsync
@MapperScan("site.xlinks.ai.router.mapper")
public class DistributedApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedApiApplication.class, args);
    }
}
