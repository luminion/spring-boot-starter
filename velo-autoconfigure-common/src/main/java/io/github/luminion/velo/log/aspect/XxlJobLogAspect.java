package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 任务入口日志适配。
 */
@Aspect
public class XxlJobLogAspect extends SourceLogAspectSupport {
    public XxlJobLogAspect(VeloProperties properties, InvocationLogEngine engine) {
        super(properties, engine, InvocationLogSource.XXL_JOB);
    }

    @Around("@annotation(com.xxl.job.core.handler.annotation.XxlJob)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        return log(point);
    }
}
