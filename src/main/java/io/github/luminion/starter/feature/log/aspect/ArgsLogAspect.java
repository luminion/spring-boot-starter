package io.github.luminion.starter.feature.log.aspect;

import io.github.luminion.starter.feature.log.InvokeArgsWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 入参日志切面
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class ArgsLogAspect {
    private final InvokeArgsWriter argsWriter;

    @Before("@annotation(io.github.luminion.starter.feature.log.annotation.ArgsLog) " +
            "|| @within(io.github.luminion.starter.feature.log.annotation.ArgsLog) " +
            "|| @annotation(io.github.luminion.starter.feature.log.annotation.InvokeLog) " +
            "|| @within(io.github.luminion.starter.feature.log.annotation.InvokeLog)")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        argsWriter.writeArgs(signature, args);
    }
}
