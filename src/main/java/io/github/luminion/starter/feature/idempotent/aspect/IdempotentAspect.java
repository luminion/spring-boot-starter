package io.github.luminion.starter.feature.idempotent.aspect;

import io.github.luminion.starter.feature.idempotent.annotation.Idempotent;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.feature.idempotent.IdempotentHandler;
import io.github.luminion.starter.feature.idempotent.exception.IdempotentException;
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
public class IdempotentAspect {
    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final IdempotentHandler idempotentHandler;

    /**
     * 构造函数
     *
     * @param prefix              方法指纹的前缀
     * @param fingerprinter 方法指纹生成器
     * @param idempotentHandler   幂等性处理器
     */
    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.idempotentHandler = idempotentHandler;
    }

    @Around("@annotation(idempotent)")
    public Object doIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 生成幂等 Key
        String key = prefix + ":" + fingerprinter.resolveMethodFingerprint(
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
