package io.github.luminion.starter.log.aspect;

import io.github.luminion.starter.log.InvokeLogWriter;
import io.github.luminion.starter.log.annotation.InvokeLog;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class InvokeLogAspect {
    private final InvokeLogWriter invokeLogWriter;

    @Around("@annotation(invokeLog)")
    public Object doAround(ProceedingJoinPoint pjp, InvokeLog invokeLog) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Object[] args = pjp.getArgs();
        Object target = pjp.getTarget();

        if (invokeLog.logArgs()) {
            invokeLogWriter.printMethodArgs(target, signature, args);
        }

        long start = System.nanoTime();
        try {
            Object result = pjp.proceed(args);
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            if (invokeLog.logResult()) {
                invokeLogWriter.printReturnValue(target, signature, args, result, duration);
            }
            return result;
        } catch (Throwable e) {
            // Optional: log error duration or just rethrow
            throw e;
        }
    }

}
