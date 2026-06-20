package io.github.luminion.velo.idempotent.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.exception.IdempotentException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * 接口幂等性切面。
 *
 * 语义是“TTL 窗口内拒绝重复提交”，而不是“方法结束后释放并发锁”。
 */
@Aspect
public class IdempotentAspect implements Ordered {
    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final IdempotentHandler idempotentHandler;

    private int order = VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT;

    public IdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.idempotentHandler = idempotentHandler;
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

        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(
                prefix,
                fingerprinter.resolveMethodFingerprint(
                        joinPoint.getTarget(),
                        method,
                        joinPoint.getArgs(),
                        idempotent.key()));

        boolean accepted = idempotentHandler.tryRecord(key, ttl, idempotent.unit());
        if (!accepted) {
            throw new IdempotentException(idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 任何下游失败时清除幂等记录，允许重试（包括限流拒绝、锁获取失败、业务异常等）
            idempotentHandler.remove(key);
            throw ex;
        }
    }
}
