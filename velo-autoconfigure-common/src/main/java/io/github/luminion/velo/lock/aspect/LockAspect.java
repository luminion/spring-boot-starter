package io.github.luminion.velo.lock.aspect;

import io.github.luminion.velo.VeloProperties;
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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 分布式锁切面。
 * <p>
 * 有意只保护当前 AOP 调用点：同步返回或抛异常即释放，不等待返回对象或方法内部的异步任务，
 * 也不等待已存在的外层事务提交。需要覆盖完整业务过程时，应由业务安排调用点与事务边界。
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
    private final String defaultMessage;

    private final int order;

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler) {
        this(prefix, fingerprinter, lockHandler, null);
    }

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler,
                      VeloMessageResolver messageResolver) {
        this(prefix, fingerprinter, lockHandler, messageResolver,
                VeloAdvisorOrder.CONCURRENCY_LOCK);
    }

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler,
                      VeloMessageResolver messageResolver, int order) {
        this(prefix, fingerprinter, lockHandler, messageResolver, order,
                new VeloProperties.LockProperties().getMessage());
    }

    /**
     * 指定默认提示信息；仅在获取锁被拒绝时选择文案并按当前语言解析。
     */
    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler,
                      VeloMessageResolver messageResolver, int order, String defaultMessage) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.lockHandler = lockHandler;
        this.messageResolver = messageResolver;
        this.defaultMessage = defaultMessage;
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
        lock = AnnotationUtils.synthesizeAnnotation(lock, method);
        // 资源 prefix 显式替代方法指纹；value 为空不拼接参数，两个都为空即为默认方法锁。
        String keyFingerprint = fingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(), method, joinPoint.getArgs(), lock.prefix(), lock.value());
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        // 2. 尝试获取锁
        boolean lockSuccess = lockHandler.tryLock(key);
        if (!lockSuccess) {
            throw new LockException(resolveMessage(lock.message()), key);
        }

        try {
            // 3. 执行业务方法
            return joinPoint.proceed();
        } finally {
            // 在获取锁的原线程释放；Future / CompletionStage 的后续完成不属于本切面的持锁范围。
            lockHandler.unlock(key);
        }
    }

    private String resolveMessage(String message) {
        // 非空白注解文案覆盖全局配置；两种来源都沿用统一的国际化解析。
        String selectedMessage = StringUtils.hasText(message) ? message : defaultMessage;
        return messageResolver != null ? messageResolver.resolve(selectedMessage) : selectedMessage;
    }
}
