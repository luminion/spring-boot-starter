package io.github.luminion.velo.lock.aspect;

import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.core.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.exception.LockException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 分页锁切面
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class LockAspect {

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final LockHandler lockHandler;

    @Around("@annotation(lock)")
    public Object doLock(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String keyExpression = lock.key();
        long wait = lock.waitTimeout();
        long lease = lock.lease();

        // 1. 生成锁 Key
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(
                prefix,
                fingerprinter.resolveMethodFingerprint(
                        joinPoint.getTarget(),
                        method,
                        joinPoint.getArgs(),
                        keyExpression));

        // 2. 尝试获取锁
        boolean lockSuccess = lockHandler.lock(key, wait, lease, lock.unit());
        if (!lockSuccess) {
            throw new LockException(lock.message());
        }

        try {
            // 3. 执行业务方法
            return joinPoint.proceed();
        } finally {
            // 4. 释放锁
            lockHandler.unlock(key);
        }
    }
}
