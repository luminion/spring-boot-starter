package io.github.luminion.velo.idempotent.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.exception.IdempotentException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 接口幂等性切面。
 *
 * 语义是“TTL 窗口内拒绝重复提交”，而不是“方法结束后释放并发锁”。
 */
@Aspect
public class IdempotentAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final IdempotentHandler idempotentHandler;
    private final VeloMessageResolver messageResolver;

    private int order = VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT;

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler) {
        this(prefix, fingerprinter, idempotentHandler, null);
    }

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
            VeloMessageResolver messageResolver) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.idempotentHandler = idempotentHandler;
        this.messageResolver = messageResolver;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@annotation(idempotent)")
    public Object doIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());
        long ttl = idempotent.ttl();
        if (ttl <= 0L) {
            throw new IllegalArgumentException("Idempotent ttl must be greater than zero.");
        }

        // 空 key 会降级为方法级幂等（类名#方法名(参数类型...)），同一方法的所有调用共享一个幂等窗口，
        // 不区分参数与调用者。这通常不是期望行为，因此打 WARN 提醒业务显式指定 key。
        if (!StringUtils.hasText(idempotent.key())) {
            log.warn("[Velo Starter] @Idempotent on {}#{} has no 'key' expression. " +
                            "It will fall back to method-level idempotency, meaning all invocations of this method " +
                            "(regardless of arguments or caller) share a single idempotency window. " +
                            "Specify a SpEL key (e.g. key=\"#userId\") unless this is intended.",
                    method.getDeclaringClass().getName(), method.getName());
        }

        // key 始终以方法指纹（类名#方法名(参数类型...)）为前缀，再拼接 SpEL 结果，与限流分桶语义保持一致。
        // 这样不同方法即便用相同的 SpEL key（如都用 #orderId）也不会互相碰撞、共享同一幂等窗口。
        String methodFingerprint = fingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(), method, joinPoint.getArgs(), "");
        String keyFingerprint = methodFingerprint;
        if (StringUtils.hasText(idempotent.key())) {
            keyFingerprint += ':' + fingerprinter.resolveMethodFingerprint(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), idempotent.key());
        }
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        // 为本次请求生成唯一 token，失败回滚时只清除自己写入的记录，避免误删并发请求的新记录。
        String token = UUID.randomUUID().toString();

        boolean accepted = idempotentHandler.tryRecord(key, token, ttl);
        if (!accepted) {
            throw new IdempotentException(resolveMessage(idempotent.message()), key, ttl);
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 任何下游失败时清除幂等记录，允许重试（包括限流拒绝、锁获取失败、业务异常等）。
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
        return messageResolver != null ? messageResolver.resolve(message) : message;
    }
}
