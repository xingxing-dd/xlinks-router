package site.xlinks.ai.router;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 管理后台应用入口
 */
@SpringBootApplication
@MapperScan({"site.xlinks.ai.router.mapper","site.xlinks.ai.router.merchant.mapper"})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
