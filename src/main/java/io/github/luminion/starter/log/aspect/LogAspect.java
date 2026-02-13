package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.annotation.Log;
import io.github.luminion.starter.log.LogWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class LogAspect {
    private final LogWriter logWriter;

    @Around("@annotation(log)")
    public Object doAround(ProceedingJoinPoint pjp, Log log) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Object[] args = pjp.getArgs();
        Object target = pjp.getTarget();
        logWriter.before(target, signature, args);
        try {
            Object result = pjp.proceed(args);
            logWriter.after(target, signature, args, result);
            return result;
        } catch (Throwable e) {
            logWriter.error(target, signature, args, e);
            throw e;
        }
    }

}
