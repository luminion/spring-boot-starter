package io.github.luminion.velo.lock.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.exception.LockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * 分布式锁切面
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
public class LockAspect implements Ordered {

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final LockHandler lockHandler;
    private final VeloMessageResolver messageResolver;

    private int order = VeloAdvisorOrder.CONCURRENCY_LOCK;

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler) {
        this(prefix, fingerprinter, lockHandler, null);
    }

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler,
            VeloMessageResolver messageResolver) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.lockHandler = lockHandler;
        this.messageResolver = messageResolver;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@annotation(lock)")
    public Object doLock(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());
        long wait = lock.waitTimeout();
        long lease = lock.lease();
        if (wait < 0L) {
            throw new IllegalArgumentException("Lock waitTimeout must not be negative.");
        }
        // -1 是看门狗特殊值（Redisson 据此自动续约），需放行；其余非正值仍非法
        if (lease <= 0L && lease != -1L) {
            throw new IllegalArgumentException("Lock lease must be greater than zero, or -1 to enable watchdog auto-renewal.");
        }

        // 1. 生成锁 Key
        // 空 key 会降级为方法级锁（类名#方法名(参数类型...)），表示"该方法全局串行"，这是一个有意义的语义，
        // 因此与 @Idempotent 不同，这里安静降级、不打告警。
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(
                prefix,
                fingerprinter.resolveMethodFingerprint(
                        joinPoint.getTarget(),
                        method,
                        joinPoint.getArgs(),
                        lock.key()));

        // 2. 尝试获取锁
        boolean lockSuccess = lockHandler.lock(key, wait, lease);
        if (!lockSuccess) {
            throw new LockException(resolveMessage(lock.message()), key, wait, lease);
        }

        try {
            // 3. 执行业务方法
            return joinPoint.proceed();
        } finally {
            // 4. 释放锁
            lockHandler.unlock(key);
        }
    }

    private String resolveMessage(String message) {
        return messageResolver != null ? messageResolver.resolve(message) : message;
    }
}
