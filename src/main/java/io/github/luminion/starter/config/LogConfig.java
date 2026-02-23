package io.github.luminion.starter.config;

import io.github.luminion.starter.log.InvokeLogWriter;
import io.github.luminion.starter.log.aspect.InvokeLogAspect;
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
    @ConditionalOnMissingBean(InvokeLogAspect.class)
    @ConditionalOnBean(InvokeLogWriter.class)
    public InvokeLogAspect logAspect(InvokeLogWriter invokeLogWriter) {
        return new InvokeLogAspect(invokeLogWriter);
    }


}
