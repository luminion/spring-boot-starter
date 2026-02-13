package io.github.luminion.starter.timer.aspect;

import io.github.luminion.starter.timer.annotation.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;

/**
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@Configuration
@Slf4j
public class TimerAspect {

    @Around("@annotation(timer)")
    public Object logTime(ProceedingJoinPoint joinPoint, Timer timer) throws Throwable {
        var value = timer.value();
        if (value.isEmpty()) {
            var methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            var signature = joinPoint.getSignature();
            value = method.getDeclaringClass().getName() + "." + signature.getName() + "()";
        }
        var stopWatch = new StopWatch();
        stopWatch.start();
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("{} => Cost: {} ms", value, stopWatch.getTotalTimeMillis());
        }
    }
}
