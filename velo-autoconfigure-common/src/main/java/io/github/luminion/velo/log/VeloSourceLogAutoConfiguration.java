package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.log.aspect.XxlJobLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 定时任务来源的调用日志自动配置。
 */
@AutoConfiguration(after = VeloLogAutoConfiguration.class)
@ConditionalOnClass(name = "org.aspectj.weaver.Advice")
@ConditionalOnProperty(
        prefix = "velo.log",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VeloSourceLogAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "velo.log.sources.scheduled",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public ScheduledLogAspect scheduledLogAspect(
            VeloProperties properties, InvocationLogEngine engine) {
        return new ScheduledLogAspect(properties, engine);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
    @ConditionalOnProperty(
            prefix = "velo.log.sources.xxl-job",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public XxlJobLogAspect xxlJobLogAspect(VeloProperties properties, InvocationLogEngine engine) {
        return new XxlJobLogAspect(properties, engine);
    }
}
