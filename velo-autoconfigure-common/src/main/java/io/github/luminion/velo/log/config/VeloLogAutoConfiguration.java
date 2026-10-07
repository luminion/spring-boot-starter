package io.github.luminion.velo.log.config;

import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.LogValueFormatter;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 共用同步调用日志引擎、对象转换和输出；不负责链路上下文。
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
        "io.github.luminion.velo.jackson.VeloJacksonLogAutoConfiguration"
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLogAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LogValueFormatter logValueFormatter() {
        return String::valueOf;
    }

    @Bean
    @ConditionalOnMissingBean
    public InvocationLogWriter invocationLogWriter() {
        return new Slf4JInvocationLogWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    public InvocationLogEngine invocationLogEngine(VeloProperties properties, LogValueFormatter formatter, InvocationLogWriter writer) {
        int maxPayloadLength = properties.getLog().getDefaults().getMaxPayloadLength();
        if (maxPayloadLength != -1 && maxPayloadLength != 0) {
            throw new IllegalArgumentException(
                    "velo.log.defaults.max-payload-length only supports -1 (unlimited) or 0 (disabled)");
        }
        validateThreshold(properties.getLog().getDefaults().getSlowLog());
        VeloProperties.InvocationSources sources = properties.getLog().getSources();
        validateThreshold(sources.getController().getSlowLog());
        validateThreshold(sources.getFeign().getSlowLog());
        validateThreshold(sources.getInvoke().getSlowLog());
        validateThreshold(sources.getScheduled().getSlowLog());
        validateThreshold(sources.getXxlJob().getSlowLog());
        return new InvocationLogEngine(properties, formatter, writer);
    }

    private void validateThreshold(VeloProperties.SlowLogProperties value) {
        if (value.getThresholdMs() != null && value.getThresholdMs() < 0) {
            throw new IllegalArgumentException("SlowLog threshold-ms must be zero or greater");
        }
    }
}
