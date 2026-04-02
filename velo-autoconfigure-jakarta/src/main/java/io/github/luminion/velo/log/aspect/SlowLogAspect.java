package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.log.SlowLogWriter;
import io.github.luminion.velo.log.annotation.SlowLog;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;

@Aspect
@RequiredArgsConstructor
public class SlowLogAspect {
    private final SlowLogWriter slowLogWriter;

    @Around("@within(io.github.luminion.velo.log.annotation.SlowLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.SlowLog)")
    public Object logTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 统一从 merged annotation 读取配置，保证方法级配置优先覆盖类级配置。
        SlowLog slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), SlowLog.class);
        if (slowLog == null) {
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

            // 统一换算成纳秒后比较，避免在不同时间单位之间写重复分支。
            if (durationNs > thresholdNs) {
                slowLogWriter.writeSlow(signature, durationNs);
            }
        }
    }
}
