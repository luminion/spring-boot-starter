package io.github.luminion.velo.idempotent.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.exception.IdempotentException;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.util.InvocationAnnotationResolver;
import java.lang.reflect.Method;
import java.util.UUID;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.util.StringUtils;

/**
 * 接口幂等性拦截器。
 * <p>
 * 语义是“TTL 窗口内拒绝重复提交”，而不是“方法结束后释放并发锁”。
 * 仅管理当前 AOP 调用点的同步异常；返回 Future / CompletionStage 或启动内部异步任务后，
 * 不等待、不监听其结果。异步对象正常返回即保留记录至 TTL 到期，后续异步失败不清理记录。
 * 未声明 value 和 prefix 时按方法范围共享同一个幂等窗口。
 */
public class IdempotentAspect implements MethodInterceptor {
    private static final InvocationAnnotationResolver<Idempotent> ANNOTATIONS =
            new InvocationAnnotationResolver<>(Idempotent.class, false);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final IdempotentHandler idempotentHandler;
    private final VeloMessageResolver messageResolver;
    private final String defaultMessage;

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler) {
        this(prefix, fingerprinter, idempotentHandler, null);
    }

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
                            VeloMessageResolver messageResolver) {
        this(prefix, fingerprinter, idempotentHandler, messageResolver,
                new VeloProperties.IdempotentProperties().getMessage());
    }

    /**
     * 指定默认提示信息；仅在重复提交被拒绝时选择文案并按当前语言解析。
     */
    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
                            VeloMessageResolver messageResolver, String defaultMessage) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.idempotentHandler = idempotentHandler;
        this.messageResolver = messageResolver;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 幂等拦截器的切点：实现方法上直接声明了 {@link Idempotent}，不搜索父接口、父类。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        return AnnotatedMethodPointcut.forDeclaredMethods(Idempotent.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        Idempotent idempotent = ANNOTATIONS.resolve(invocation.getMethod(), target);
        if (idempotent == null) {
            return invocation.proceed();
        }
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(target, invocation.getMethod());
        long ttl = idempotent.ttl();
        if (ttl <= 0L) {
            throw new IllegalArgumentException("Idempotent ttl must be greater than zero.");
        }

        // 默认隔离不同方法；显式资源 prefix 使不同方法可共享同一业务防重复窗口。
        String keyFingerprint = fingerprinter.resolveMethodFingerprint(
                target, method, invocation.getArguments(), idempotent.prefix(), idempotent.value());
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        // 为本次请求生成唯一 token，失败回滚时只清除自己写入的记录，避免误删并发请求的新记录。
        String token = UUID.randomUUID().toString();

        boolean accepted = idempotentHandler.tryRecord(key, token, ttl);
        if (!accepted) {
            throw new IdempotentException(resolveMessage(idempotent.message()), key, ttl);
        }

        try {
            return invocation.proceed();
        } catch (Throwable ex) {
            // 当前调用同步抛出异常时清除记录，允许重试（包括限流拒绝、锁获取失败、业务异常等）。
            // 不挂接异步完成回调：后续异步失败不属于这里的失败清理范围，这是调用点约定。
            // removeIfMatch 只删除与本次 token 一致的记录，避免误删并发请求刚写入的记录。
            try {
                idempotentHandler.removeIfMatch(key, token);
            } catch (Throwable cleanupEx) {
                // 清理失败（如 Redis 超时）不能覆盖原始业务异常，附加到 suppressed 上保留现场
                ex.addSuppressed(cleanupEx);
            }
            throw ex;
        }
    }

    private String resolveMessage(String message) {
        // 非空白注解文案覆盖全局配置；两种来源都沿用统一的国际化解析。
        String selectedMessage = StringUtils.hasText(message) ? message : defaultMessage;
        return messageResolver != null ? messageResolver.resolve(selectedMessage) : selectedMessage;
    }
}
