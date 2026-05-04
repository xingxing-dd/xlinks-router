package site.xlinks.ai.router.distributed.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import site.xlinks.ai.router.distributed.protocol.interceptor.ProtocolCustomerTokenInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ProtocolCustomerTokenInterceptor protocolCustomerTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(protocolCustomerTokenInterceptor)
                .addPathPatterns("/v1/**");
    }
}
