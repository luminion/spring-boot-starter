package io.github.luminion.starter.config;

import io.github.luminion.starter.log.LogWriter;
import io.github.luminion.starter.log.aspect.LogAspect;
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
    @ConditionalOnMissingBean(LogAspect.class)
    @ConditionalOnBean(LogWriter.class)
    public LogAspect logAspect(LogWriter logWriter) {
        return new LogAspect(logWriter);
    }


}
