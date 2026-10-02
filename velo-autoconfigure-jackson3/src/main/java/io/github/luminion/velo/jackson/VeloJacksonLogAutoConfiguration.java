package io.github.luminion.velo.jackson;

import io.github.luminion.velo.log.LogValueFormatter;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * 使用应用现有 Mapper 的配置；不依赖 MVC，不新建或修改 Mapper。
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
                "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"
        },
        before = VeloLogAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
public class VeloJacksonLogAutoConfiguration {
    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnMissingBean(LogValueFormatter.class)
    public LogValueFormatter jacksonLogValueFormatter(ObjectMapper mapper) {
        return value -> {
            try {
                return mapper.writeValueAsString(value);
            } catch (Exception error) {
                throw new IllegalArgumentException("Jackson log value serialization failed", error);
            }
        };
    }
}
