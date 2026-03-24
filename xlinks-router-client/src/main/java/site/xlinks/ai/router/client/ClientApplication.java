package site.xlinks.ai.router.client;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import site.xlinks.ai.router.client.config.SmsProperties;

@SpringBootApplication
@ComponentScan("site.xlinks.ai.router")
@MapperScan({"site.xlinks.ai.router.mapper"})
@EnableConfigurationProperties(SmsProperties.class)
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
