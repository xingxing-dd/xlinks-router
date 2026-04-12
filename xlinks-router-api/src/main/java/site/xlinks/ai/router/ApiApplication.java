package site.xlinks.ai.router;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 瀵瑰 API 搴旂敤鍏ュ彛
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("site.xlinks.ai.router.mapper")
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
