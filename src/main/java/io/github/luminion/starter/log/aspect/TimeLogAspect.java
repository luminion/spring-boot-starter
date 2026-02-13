package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.annotation.TimeLog;
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
public class TimeLogAspect {

    @Around("@annotation(timeLog)")
    public Object logTime(ProceedingJoinPoint joinPoint, TimeLog timeLog) throws Throwable {
        String value = timeLog.value();
        if (value.isEmpty()) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            value = method.getDeclaringClass().getName() + "." + methodSignature.getName() + "()";
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            long timeMillis = stopWatch.getTotalTimeMillis();
            if (timeMillis > timeLog.threshold()){
                switch (timeLog.level()){
                    case TRACE :{
                        log.trace("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case DEBUG :{
                        log.debug("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case INFO :{
                        log.info("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case WARN :{
                        log.warn("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                    case ERROR :{
                        log.error("{} => Time Cost: {} ms", value, timeMillis);
                        break;
                    }
                }
            }
        }
    }
}
