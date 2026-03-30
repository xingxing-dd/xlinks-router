package site.xlinks.ai.router.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JacksonIdSerializerConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonIdSerializerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.setSerializerModifier(new BeanSerializerModifier() {
                @Override
                public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                                 BeanDescription beanDesc,
                                                                 List<BeanPropertyWriter> beanProperties) {
                    for (BeanPropertyWriter writer : beanProperties) {
                        if (isIdField(writer.getName()) && isLongType(writer.getType())) {
                            writer.assignSerializer(ToStringSerializer.instance);
                        }
                    }
                    return beanProperties;
                }
            });
            builder.modulesToInstall(module);
        };
    }

    private boolean isIdField(String name) {
        return "id".equals(name) || name.endsWith("Id");
    }

    private boolean isLongType(JavaType type) {
        Class<?> raw = type.getRawClass();
        return Long.class.equals(raw) || Long.TYPE.equals(raw);
    }
}
