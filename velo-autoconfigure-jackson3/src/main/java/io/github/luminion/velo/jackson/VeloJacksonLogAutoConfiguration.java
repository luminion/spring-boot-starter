package io.github.luminion.velo.jackson;

import io.github.luminion.velo.log.LogValueFormatter;
import io.github.luminion.velo.log.config.VeloLogAutoConfiguration;
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

    /**
     * 以应用的 Jackson Mapper 格式化调用日志值。
     *
     * @param mapper 应用使用的 JSON Mapper
     * @return 日志值格式化器
     */
    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnMissingBean(LogValueFormatter.class)
    public LogValueFormatter jacksonLogValueFormatter(ObjectMapper mapper) {
        return mapper::writeValueAsString;
    }
}
