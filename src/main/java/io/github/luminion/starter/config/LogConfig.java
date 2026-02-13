package io.github.luminion.starter.config;

import io.github.luminion.starter.log.MethodLogWriter;
import io.github.luminion.starter.log.aspect.MethodLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 日志配置
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnMissingBean
public class LogConfig {

    @Bean
    @ConditionalOnMissingBean(MethodLogAspect.class)
    @ConditionalOnBean(MethodLogWriter.class)
    public MethodLogAspect logAspect(MethodLogWriter methodLogWriter) {
        return new MethodLogAspect(methodLogWriter);
    }


}
