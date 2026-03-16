package site.xlinks.ai.router.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

 /*   @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("xlinks-router Admin API")
                        .description("xlinks-router 管理后台 API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("xlinks")
                                .email("support@xlinks.ai")));
    }
*/
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("xlinks-router Admin API")
                        .description("Spring Boot 集成 Springdoc")
                        .version("1.0")
                        .termsOfService("")
                        .contact(new Contact()
                                .name("xlinks")
                                .url("")
                                .email("support@xlinks.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .packagesToScan("site.xlinks.ai.router")
                .build();
    }
}
