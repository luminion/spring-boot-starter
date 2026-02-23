package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.annotation.SlowLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class SlowLogAspect {

    @Around("@annotation(slowLog)")
    public Object logTime(ProceedingJoinPoint joinPoint, SlowLog slowLog) throws Throwable {
        String name = slowLog.name().isEmpty() ? slowLog.value() : slowLog.name();
        if (name.isEmpty()) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            name = method.getDeclaringClass().getSimpleName() + "." + methodSignature.getName() + "()";
        }

        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationNs = System.nanoTime() - start;
            long thresholdNs = slowLog.timeUnit().toNanos(slowLog.threshold());

            if (durationNs > thresholdNs) {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNs);
                String unit = slowLog.timeUnit().name().toLowerCase();
                log(slowLog.level(), "{} => Time Cost: {} ms (threshold: {} {})", name, durationMs, slowLog.threshold(),
                        unit);
            }
        }
    }

    private void log(Level level, String format, Object... arguments) {
        switch (level) {
            case TRACE:
                log.trace(format, arguments);
                break;
            case DEBUG:
                log.debug(format, arguments);
                break;
            case INFO:
                log.info(format, arguments);
                break;
            case WARN:
                log.warn(format, arguments);
                break;
            case ERROR:
                log.error(format, arguments);
                break;
        }
    }
}
