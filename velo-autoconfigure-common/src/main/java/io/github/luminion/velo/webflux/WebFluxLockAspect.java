package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.ReactiveTypeSupport;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.lock.LockToken;
import io.github.luminion.velo.lock.ReactiveLockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.exception.LockException;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * WebFlux 响应式锁切面。
 *
 * <p>锁在订阅时获取，在响应式链完成、异常或取消时释放。释放使用后端返回的所有权令牌，
 * 不依赖完成信号所在的 Reactor 线程。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Aspect
public class WebFluxLockAspect implements Ordered {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebFluxLockAspect.class);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final io.github.luminion.velo.lock.LockHandler lockHandler;
    private final VeloMessageResolver messageResolver;

    private int order = VeloAdvisorOrder.CONCURRENCY_LOCK;

    public WebFluxLockAspect(String prefix, Fingerprinter fingerprinter,
            io.github.luminion.velo.lock.LockHandler lockHandler, VeloMessageResolver messageResolver) {
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
        if (!ReactiveTypeSupport.isReactiveType(signature.getReturnType())) {
            return joinPoint.proceed();
        }

        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());
        long wait = lock.waitTimeout();
        long lease = lock.lease();
        if (wait < 0L) {
            throw new IllegalArgumentException("Lock waitTimeout must not be negative.");
        }
        if (lease <= 0L && lease != -1L) {
            throw new IllegalArgumentException("Lock lease must be greater than zero, or -1 to enable watchdog auto-renewal.");
        }

        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(
                prefix,
                fingerprinter.resolveMethodFingerprint(
                        joinPoint.getTarget(), method, joinPoint.getArgs(), lock.key()));

        if (WebFluxReactiveSupport.isMonoReturnType(signature.getReturnType())) {
            return applyMono(joinPoint, lock, key, wait, lease, lock.message());
        }
        return applyFlux(joinPoint, lock, key, wait, lease, lock.message());
    }

    private Mono<?> applyMono(ProceedingJoinPoint joinPoint, Lock lock, String key, long wait, long lease,
            String message) {
        return Mono.defer(() -> acquire(key, wait, lease, message))
                .flatMap(token -> Mono.usingWhen(
                        Mono.just(token),
                        ignored -> WebFluxReactiveSupport.proceedMono(joinPoint),
                        this::release,
                        (ignored, error) -> release(ignored),
                        this::release));
    }

    private Flux<?> applyFlux(ProceedingJoinPoint joinPoint, Lock lock, String key, long wait, long lease,
            String message) {
        return Flux.defer(() -> acquire(key, wait, lease, message)
                .flatMapMany(token -> Flux.usingWhen(
                        Mono.just(token),
                        ignored -> WebFluxReactiveSupport.proceedFlux(joinPoint),
                        this::release,
                        (ignored, error) -> release(ignored),
                        this::release)));
    }

    private Mono<LockToken> acquire(String key, long wait, long lease, String message) {
        return WebFluxReactiveSupport.fromStage(() -> {
            if (!(lockHandler instanceof ReactiveLockHandler)) {
                throw new IllegalStateException("WebFlux @Lock requires a LockHandler that implements ReactiveLockHandler. " +
                        "Use a Velo built-in lock handler or implement token-based reactive ownership.");
            }
            return ((ReactiveLockHandler) lockHandler).lockToken(key, wait, lease);
        }).flatMap(token -> token == null
                ? Mono.error(new LockException(resolveMessage(message), key, wait, lease))
                : Mono.just(token));
    }

    private Mono<Void> release(LockToken token) {
        if (!(lockHandler instanceof ReactiveLockHandler)) {
            return Mono.empty();
        }
        return WebFluxReactiveSupport.fromStage(() -> ((ReactiveLockHandler) lockHandler).unlockToken(token))
                .onErrorResume(error -> {
                    log.warn("[Velo Starter] Reactive lock release failed for key '{}': {}", token.getKey(),
                            error.toString());
                    return Mono.empty();
                });
    }

    private String resolveMessage(String message) {
        return messageResolver != null ? messageResolver.resolve(message) : message;
    }
}
