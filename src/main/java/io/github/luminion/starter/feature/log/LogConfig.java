package io.github.luminion.starter.feature.log;

import io.github.luminion.starter.feature.log.aspect.ArgsLogAspect;
import io.github.luminion.starter.feature.log.aspect.ErrorLogAspect;
import io.github.luminion.starter.feature.log.aspect.ResultLogAspect;
import io.github.luminion.starter.feature.log.aspect.SlowLogAspect;
import io.github.luminion.starter.feature.log.support.Slf4JLogWriter;
import org.aspectj.weaver.Advice;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 日志自动配置
 *
 * @see org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ Advice.class })
public class LogConfig {

    @Bean
    @ConditionalOnMissingBean({ InvokeArgsWriter.class, InvokeResultWriter.class, SlowLogWriter.class, ErrorLogWriter.class })
    public Slf4JLogWriter slf4JLogWriter() {
        return new Slf4JLogWriter(Level.DEBUG);
    }

    @Bean
    @ConditionalOnMissingBean(ArgsLogAspect.class)
    @ConditionalOnBean(InvokeArgsWriter.class)
    public ArgsLogAspect argsLogAspect(InvokeArgsWriter argsWriter) {
        return new ArgsLogAspect(argsWriter);
    }

    @Bean
    @ConditionalOnMissingBean(ResultLogAspect.class)
    @ConditionalOnBean(InvokeResultWriter.class)
    public ResultLogAspect resultLogAspect(InvokeResultWriter resultWriter) {
        return new ResultLogAspect(resultWriter);
    }

    @Bean
    @ConditionalOnMissingBean(ErrorLogAspect.class)
    @ConditionalOnBean(ErrorLogWriter.class)
    public ErrorLogAspect errorLogAspect(ErrorLogWriter errorLogWriter) {
        return new ErrorLogAspect(errorLogWriter);
    }

    @Bean
    @ConditionalOnMissingBean(SlowLogAspect.class)
    @ConditionalOnBean(SlowLogWriter.class)
    public SlowLogAspect slowLogAspect(SlowLogWriter slowLogWriter) {
        return new SlowLogAspect(slowLogWriter);
    }
}
