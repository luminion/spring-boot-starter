package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.log.InvokeArgsWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@RequiredArgsConstructor
public class ArgsLogAspect {
    private final InvokeArgsWriter argsWriter;

    @Before("@annotation(io.github.luminion.velo.log.annotation.ArgsLog) " +
            "|| @within(io.github.luminion.velo.log.annotation.ArgsLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.InvokeLog) " +
            "|| @within(io.github.luminion.velo.log.annotation.InvokeLog)")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        argsWriter.writeArgs(signature, args);
    }
}
