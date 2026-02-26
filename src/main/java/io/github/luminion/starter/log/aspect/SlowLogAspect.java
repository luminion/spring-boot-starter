package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.SlowLogWriter;
import io.github.luminion.starter.log.annotation.SlowLog;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 慢日志切面
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class SlowLogAspect {
    private final SlowLogWriter slowLogWriter;

    @Around("@within(io.github.luminion.starter.log.annotation.SlowLog) " +
            "|| @annotation(io.github.luminion.starter.log.annotation.SlowLog)")
    public Object logTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 核心逻辑：获取注解实例，支持方法级覆盖类级
        SlowLog slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), SlowLog.class);
        if (slowLog == null) {
            // 如果方法上没有，尝试从类上获取 (兜底，虽然切点已经过滤，但为了保险)
            slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getDeclaringType(), SlowLog.class);
        }

        if (slowLog == null) {
            return joinPoint.proceed();
        }

        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationNs = System.nanoTime() - start;
            long thresholdNs = slowLog.timeUnit().toNanos(slowLog.value());

            if (durationNs > thresholdNs) {
                slowLogWriter.writeSlow(signature, durationNs);
            }
        }
    }
}
