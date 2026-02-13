package io.github.luminion.starter.core.aspect;

import io.github.luminion.starter.core.annotation.Idempotent;
import io.github.luminion.starter.core.spi.MethodFingerprinter;
import io.github.luminion.starter.core.spi.IdempotentHandler;
import io.github.luminion.starter.core.exception.IdempotentException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 接口幂等性切面
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private final MethodFingerprinter methodFingerprinter;
    private final IdempotentHandler idempotentHandler;

    @Around("@annotation(idempotent)")
    public Object doIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 生成幂等 Key
        String key = "idempotent:" + methodFingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                idempotent.value());

        // 2. 尝试抢占
        boolean lock = idempotentHandler.tryLock(key, idempotent.timeout(), idempotent.unit());
        if (!lock) {
            throw new IdempotentException(idempotent.message());
        }

        try {
            // 3. 执行业务方法
            return joinPoint.proceed();
        } finally {
            // 4. 根据配置决定是否在执行完后立即释放锁
            if (idempotent.autoRelease()) {
                idempotentHandler.release(key);
            }
        }
    }
}
