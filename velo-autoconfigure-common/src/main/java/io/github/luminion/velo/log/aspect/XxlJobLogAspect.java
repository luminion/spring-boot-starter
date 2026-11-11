package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.trace.TraceScopeManager;
import io.github.luminion.velo.trace.W3cTraceContextResolver;
import org.springframework.aop.Pointcut;

/**
 * XXL-Job 任务入口日志适配。
 */
public class XxlJobLogAspect extends SourceLogAspectSupport {
    private static final String XXL_JOB = "com.xxl.job.core.handler.annotation.XxlJob";

    public XxlJobLogAspect(VeloProperties properties, InvocationLogEngine engine) {
        this(engine, new TraceScopeManager(properties.getLog().getTrace(), new W3cTraceContextResolver()));
    }

    public XxlJobLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        super(engine, trace, InvocationLogSource.XXL_JOB);
    }

    /**
     * XXL-Job 任务入口切点：方法上声明了 {@code @XxlJob}；按类名匹配，不要求加载 XXL-Job 类。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        return AnnotatedMethodPointcut.forMethods(XXL_JOB);
    }
}
