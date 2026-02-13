package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.annotation.TimeLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@Configuration
@Slf4j
public class TimeLogAspect {

    @Around("@annotation(timeLog)")
    public Object logTime(ProceedingJoinPoint joinPoint, TimeLog timeLog) throws Throwable {
        String value = timeLog.value();
        if (value.isEmpty()) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            value = method.getDeclaringClass().getName() + "." + methodSignature.getName() + "()";
        }
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            // 转换为毫秒，保留更精确的小数或取整
            long timeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (timeMillis > timeLog.threshold()) {
                switch (timeLog.level()) {
                    case TRACE: {
                        log.trace("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case DEBUG: {
                        log.debug("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case INFO: {
                        log.info("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case WARN: {
                        log.warn("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case ERROR: {
                        log.error("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                }
            }
        }
    }
}
