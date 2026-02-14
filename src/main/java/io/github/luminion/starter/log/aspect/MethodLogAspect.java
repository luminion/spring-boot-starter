package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.annotation.MethodLog;
import io.github.luminion.starter.log.MethodLogWriter;
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
public class MethodLogAspect {
    private final MethodLogWriter methodLogWriter;

    @Around("@annotation(methodLog)")
    public Object doAround(ProceedingJoinPoint pjp, MethodLog methodLog) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Object[] args = pjp.getArgs();
        Object target = pjp.getTarget();
        methodLogWriter.printMethodArgs(target, signature, args);
        Object result = pjp.proceed(args);
        methodLogWriter.printReturnValue(target, signature, args, result);
        return result;
    }

}
