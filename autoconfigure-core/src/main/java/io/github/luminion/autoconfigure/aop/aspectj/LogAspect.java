package io.github.luminion.autoconfigure.aop.aspectj;

import io.github.luminion.autoconfigure.aop.spi.LogWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StopWatch;

import java.util.Collection;


/**
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class LogAspect {
    private final Collection<String> logExcludeProperties;
    private final LogWriter logWriter;

    @Pointcut("@annotation(io.github.luminion.autoconfigure.aop.annotation.Log)")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Object[] args = pjp.getArgs();
        Object target = pjp.getTarget();
        logWriter.before(target, signature, args);
        stopWatch.start();
        try {
            Object result = pjp.proceed(args);
            stopWatch.stop();
            logWriter.after(target, signature, args, result, stopWatch.getTotalTimeMillis());
            return result;
        } catch (Throwable e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            logWriter.error(target, signature, args, e, stopWatch.getTotalTimeMillis());
            throw e;
        }
    }

}
