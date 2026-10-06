package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.log.LogValueFormatter;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

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
        return (value, output) -> mapper.writeValue(output, value);
    }
}
