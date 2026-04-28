package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.aspect.ArgsLogAspect;
import io.github.luminion.velo.log.aspect.ErrorLogAspect;
import io.github.luminion.velo.log.aspect.ResultLogAspect;
import io.github.luminion.velo.log.aspect.SlowLogAspect;
import io.github.luminion.velo.log.support.Slf4JLogWriter;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Slf4JLogWriter.class)
    public Slf4JLogWriter slf4JLogWriter(VeloProperties properties) {
        return new Slf4JLogWriter(toLevel(properties.getLog().getLevel()));
    }

    @Bean
    @ConditionalOnMissingBean(ArgsLogAspect.class)
    @ConditionalOnBean(InvokeArgsWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "args-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public ArgsLogAspect argsLogAspect(ObjectProvider<InvokeArgsWriter> argsWriters) {
        return new ArgsLogAspect(resolveWriter(argsWriters, InvokeArgsWriter.class));
    }

    @Bean
    @ConditionalOnMissingBean(ResultLogAspect.class)
    @ConditionalOnBean(InvokeResultWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "result-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public ResultLogAspect resultLogAspect(ObjectProvider<InvokeResultWriter> resultWriters) {
        return new ResultLogAspect(resolveWriter(resultWriters, InvokeResultWriter.class));
    }

    @Bean
    @ConditionalOnMissingBean(ErrorLogAspect.class)
    @ConditionalOnBean(ErrorLogWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "error-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public ErrorLogAspect errorLogAspect(ObjectProvider<ErrorLogWriter> errorLogWriters) {
        return new ErrorLogAspect(resolveWriter(errorLogWriters, ErrorLogWriter.class));
    }

    @Bean
    @ConditionalOnMissingBean(SlowLogAspect.class)
    @ConditionalOnBean(SlowLogWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "slow-aspect-enabled", havingValue = "true", matchIfMissing = true)
    public SlowLogAspect slowLogAspect(ObjectProvider<SlowLogWriter> slowLogWriters) {
        return new SlowLogAspect(resolveWriter(slowLogWriters, SlowLogWriter.class));
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

    private <T> T resolveWriter(ObjectProvider<T> writerProvider, Class<T> writerType) {
        List<T> candidates = writerProvider.orderedStream().collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No " + writerType.getSimpleName() + " bean available.");
        }
        List<T> customCandidates = candidates.stream()
                .filter(candidate -> candidate.getClass() != Slf4JLogWriter.class)
                .collect(Collectors.toList());

        if (customCandidates.size() > 1) {
            throw new NoUniqueBeanDefinitionException(writerType, customCandidates.size(),
                    "Multiple custom " + writerType.getSimpleName()
                            + " beans found; keep only one custom writer for this interface.");
        }
        if (!customCandidates.isEmpty()) {
            return customCandidates.get(0);
        }
        return candidates.get(0);
    }
}
