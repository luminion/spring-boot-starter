package io.github.luminion.velo.lock.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.exception.LockException;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.util.InvocationAnnotationResolver;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.util.StringUtils;

/**
 * 分布式锁拦截器。
 * <p>
 * 有意只保护当前 AOP 调用点：同步返回或抛异常即释放，不等待返回对象或方法内部的异步任务，
 * 也不等待已存在的外层事务提交。需要覆盖完整业务过程时，应由业务安排调用点与事务边界。
 *
 * @author luminion
 * @since 1.0.0
 */
public class LockAspect implements MethodInterceptor {
    private static final InvocationAnnotationResolver<Lock> ANNOTATIONS =
            new InvocationAnnotationResolver<>(Lock.class, false);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final LockHandler lockHandler;
    private final VeloMessageResolver messageResolver;
    private final String defaultMessage;

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler) {
        this(prefix, fingerprinter, lockHandler, null);
    }

    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler,
                      VeloMessageResolver messageResolver) {
        this(prefix, fingerprinter, lockHandler, messageResolver, new VeloProperties.LockProperties().getMessage());
    }

    /**
     * 指定默认提示信息；仅在获取锁被拒绝时选择文案并按当前语言解析。
     */
    public LockAspect(String prefix, Fingerprinter fingerprinter, LockHandler lockHandler,
                      VeloMessageResolver messageResolver, String defaultMessage) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.lockHandler = lockHandler;
        this.messageResolver = messageResolver;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 锁拦截器的切点：实现方法上直接声明了 {@link Lock}，不搜索父接口、父类。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        return AnnotatedMethodPointcut.forDeclaredMethods(Lock.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        Lock lock = ANNOTATIONS.resolve(invocation.getMethod(), target);
        if (lock == null) {
            return invocation.proceed();
        }
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(target, invocation.getMethod());
        // 资源 prefix 显式替代方法指纹；value 为空不拼接参数，两个都为空即为默认方法锁。
        String keyFingerprint = fingerprinter.resolveMethodFingerprint(
                target, method, invocation.getArguments(), lock.prefix(), lock.value());
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        if (!lockHandler.tryLock(key)) {
            throw new LockException(resolveMessage(lock.message()), key);
        }
        try {
            return invocation.proceed();
        } finally {
            // 在获取锁的原线程释放；Future / CompletionStage 的后续完成不属于本拦截器的持锁范围。
            lockHandler.unlock(key);
        }
    }

    private String resolveMessage(String message) {
        // 非空白注解文案覆盖全局配置；两种来源都沿用统一的国际化解析。
        String selectedMessage = StringUtils.hasText(message) ? message : defaultMessage;
        return messageResolver != null ? messageResolver.resolve(selectedMessage) : selectedMessage;
    }
}
