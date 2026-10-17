package io.github.luminion.velo.log.config;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.spi.InvocationLogContextFormatter;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.InvocationLogWriter;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.writer.Slf4JInvocationLogWriter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 共用调用协调器及扩展接入；按功能、按入口分别替换默认处理器。
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
        "io.github.luminion.velo.jackson.VeloJacksonLogAutoConfiguration"
})
@ConditionalOnClass(name = "org.aspectj.weaver.Advice")
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
    public InvocationLogEngine invocationLogEngine(VeloProperties properties, LogValueFormatter formatter,
            InvocationLogWriter writer, ObjectProvider<InvocationLogHandler> handlers,
            ObjectProvider<InvocationLogContextFormatter> contexts) {
        List<InvocationLogHandler> customHandlers = handlers.stream().collect(Collectors.toList());
        List<InvocationLogContextFormatter> customContexts = contexts.stream().collect(Collectors.toList());
        return new InvocationLogEngine(properties, formatter, writer, customHandlers, customContexts);
    }
}
