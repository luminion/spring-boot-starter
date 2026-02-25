package io.github.luminion.starter.feature.log.aspect;

import io.github.luminion.starter.feature.log.InvokeResultWriter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 返回值日志切面
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class ResultLogAspect {
    private final InvokeResultWriter resultWriter;

    @AfterReturning(pointcut = "@annotation(io.github.luminion.starter.feature.log.annotation.ResultLog) " +
            "|| @within(io.github.luminion.starter.feature.log.annotation.ResultLog) " +
            "|| @annotation(io.github.luminion.starter.feature.log.annotation.InvokeLog) " +
            "|| @within(io.github.luminion.starter.feature.log.annotation.InvokeLog)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        resultWriter.writeResult(signature, result);
    }
}
