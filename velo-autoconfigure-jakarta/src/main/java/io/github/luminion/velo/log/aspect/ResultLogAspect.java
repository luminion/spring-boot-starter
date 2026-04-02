package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.log.InvokeResultWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@RequiredArgsConstructor
public class ResultLogAspect {
    private final InvokeResultWriter resultWriter;

    @AfterReturning(pointcut = "@annotation(io.github.luminion.velo.log.annotation.ResultLog) " +
            "|| @within(io.github.luminion.velo.log.annotation.ResultLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.InvokeLog) " +
            "|| @within(io.github.luminion.velo.log.annotation.InvokeLog)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        resultWriter.writeResult(signature, result);
    }
}
