package io.github.luminion.velo.log;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.log.aspect.ArgsLogAspect;
import io.github.luminion.velo.log.aspect.ErrorLogAspect;
import io.github.luminion.velo.log.aspect.ResultLogAspect;
import io.github.luminion.velo.log.aspect.SlowLogAspect;
import io.github.luminion.velo.log.support.Slf4JLogWriter;
import org.aspectj.weaver.Advice;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.annotation.Bean;

/**
 * 日志自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean({InvokeArgsWriter.class, InvokeResultWriter.class, SlowLogWriter.class, ErrorLogWriter.class})
    @ConditionalOnProperty(prefix = "velo.log", name = "slf4j-log-writer-enabled", havingValue = "true", matchIfMissing = true)
    public Slf4JLogWriter slf4JLogWriter(VeloProperties properties) {
        return new Slf4JLogWriter(toLevel(properties.getLogLevel()));
    }

    @Bean
    @ConditionalOnMissingBean(ArgsLogAspect.class)
    @ConditionalOnBean(InvokeArgsWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "args-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public ArgsLogAspect argsLogAspect(InvokeArgsWriter argsWriter) {
        return new ArgsLogAspect(argsWriter);
    }

    @Bean
    @ConditionalOnMissingBean(ResultLogAspect.class)
    @ConditionalOnBean(InvokeResultWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "result-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public ResultLogAspect resultLogAspect(InvokeResultWriter resultWriter) {
        return new ResultLogAspect(resultWriter);
    }

    @Bean
    @ConditionalOnMissingBean(ErrorLogAspect.class)
    @ConditionalOnBean(ErrorLogWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "error-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public ErrorLogAspect errorLogAspect(ErrorLogWriter errorLogWriter) {
        return new ErrorLogAspect(errorLogWriter);
    }

    @Bean
    @ConditionalOnMissingBean(SlowLogAspect.class)
    @ConditionalOnBean(SlowLogWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "slow-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public SlowLogAspect slowLogAspect(SlowLogWriter slowLogWriter) {
        return new SlowLogAspect(slowLogWriter);
    }

    private Level toLevel(LogLevel logLevel) {
        if (logLevel == null) {
            return Level.INFO;
        }
        switch (logLevel) {
            case TRACE:
                return Level.TRACE;
            case DEBUG:
                return Level.DEBUG;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARN;
            case ERROR:
            case FATAL:
                return Level.ERROR;
            case OFF:
                return null;
            default:
                return Level.INFO;
        }
    }
}
