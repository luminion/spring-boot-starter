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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 接口幂等性切面。
 * <p>
 * 语义是“TTL 窗口内拒绝重复提交”，而不是“方法结束后释放并发锁”。
 * 仅管理当前 AOP 调用点的同步异常；返回 Future / CompletionStage 或启动内部异步任务后，
 * 不等待、不监听其结果。异步对象正常返回即保留记录至 TTL 到期，后续异步失败不清理记录。
 */
@Aspect
public class IdempotentAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final IdempotentHandler idempotentHandler;
    private final VeloMessageResolver messageResolver;

    private final int order;

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler) {
        this(prefix, fingerprinter, idempotentHandler, null);
    }

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
                            VeloMessageResolver messageResolver) {
        this(prefix, fingerprinter, idempotentHandler, messageResolver,
                VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT);
    }

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
                            VeloMessageResolver messageResolver, int order) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.idempotentHandler = idempotentHandler;
        this.messageResolver = messageResolver;
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
        idempotent = AnnotationUtils.synthesizeAnnotation(idempotent, method);
        long ttl = idempotent.ttl();
        if (ttl <= 0L) {
            throw new IllegalArgumentException("Idempotent ttl must be greater than zero.");
        }

        // 默认方法范围且没有表达式时，所有调用者共享窗口；显式 prefix 已声明资源共享意图，不告警。
        if (!StringUtils.hasText(idempotent.prefix()) && !StringUtils.hasText(idempotent.value())) {
            log.warn("[Velo Starter] @Idempotent on {}#{} has no 'value' expression. " +
                            "It will fall back to method-level idempotency, meaning all invocations of this method " +
                            "(regardless of arguments or caller) share a single idempotency window. " +
                            "Specify a SpEL value (e.g. value=\"#userId\") unless this is intended.",
                    method.getDeclaringClass().getName(), method.getName());
        }

        // 默认隔离不同方法；显式资源 prefix 使不同方法可共享同一业务防重复窗口。
        String keyFingerprint = fingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(), method, joinPoint.getArgs(), idempotent.prefix(), idempotent.value());
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
        return messageResolver != null ? messageResolver.resolve(message) : message;
    }
}
