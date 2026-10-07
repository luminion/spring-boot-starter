package io.github.luminion.velo.log.config;

import io.github.luminion.velo.log.core.InvocationLogEngine;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.log.aspect.XxlJobLogAspect;
import io.github.luminion.velo.log.condition.ConditionalOnInvocationAdapter;
import io.github.luminion.velo.trace.TraceScopeManager;
import io.github.luminion.velo.trace.VeloTraceAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 普通方法和任务入口适配；日志和 trace 独立启停。
 */
@AutoConfiguration(after = {VeloLogAutoConfiguration.class, VeloTraceAutoConfiguration.class})
@ConditionalOnClass(name = "org.aspectj.weaver.Advice")
public class VeloSourceLogAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnInvocationAdapter(source = "invoke")
    public InvokeLogAspect invokeLogAspect(VeloProperties properties, ObjectProvider<InvocationLogEngine> engine, ObjectProvider<TraceScopeManager> trace) {
        return new InvokeLogAspect(engine.getIfAvailable(), trace.getIfAvailable(), properties.getAspectOrder().getInvokeLog());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnInvocationAdapter(source = "scheduled")
    public ScheduledLogAspect scheduledLogAspect(ObjectProvider<InvocationLogEngine> engine, ObjectProvider<TraceScopeManager> trace) {
        return new ScheduledLogAspect(engine.getIfAvailable(), trace.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
    @ConditionalOnInvocationAdapter(source = "xxl-job")
    public XxlJobLogAspect xxlJobLogAspect(ObjectProvider<InvocationLogEngine> engine, ObjectProvider<TraceScopeManager> trace) {
        return new XxlJobLogAspect(engine.getIfAvailable(), trace.getIfAvailable());
    }
}
