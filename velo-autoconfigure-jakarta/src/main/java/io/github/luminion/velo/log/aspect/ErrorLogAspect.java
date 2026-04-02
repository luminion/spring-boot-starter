package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.log.ErrorLogWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 异常日志切面
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class ErrorLogAspect {
    private final ErrorLogWriter errorLogWriter;

    @AfterThrowing(pointcut = "@annotation(io.github.luminion.velo.log.annotation.ErrorLog) " +
            "|| @within(io.github.luminion.velo.log.annotation.ErrorLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.InvokeLog) " +
            "|| @within(io.github.luminion.velo.log.annotation.InvokeLog)", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Throwable e) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        errorLogWriter.writeError(signature, args, e);
    }
}
