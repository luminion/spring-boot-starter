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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebFlux 响应式锁切面。
 *
 * <p>锁在订阅时获取，在响应式链完成、异常或取消时释放。释放使用后端返回的所有权令牌，
 * 不依赖完成信号所在的 Reactor 线程。订阅在 token 产出前被取消时，由获取链的补偿逻辑
 * 释放已写入 Redis 的令牌，避免看门狗无限续期导致锁泄漏。</p>
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
        // 响应式链可能在 token 已写入 Redis（看门狗已启动）之后、交付给 usingWhen 之前被取消
        // （客户端断开是 WebFlux 常态）。token 产出必须与补偿登记绑定在同一个回调里完成，
        // 否则取消窗口内的锁将被看门狗无限续期，直到进程重启。
        AtomicReference<LockToken> undelivered = new AtomicReference<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        return WebFluxReactiveSupport.fromStage(() -> {
            if (!(lockHandler instanceof ReactiveLockHandler)) {
                throw new IllegalStateException("WebFlux @Lock requires a LockHandler that implements ReactiveLockHandler. " +
                        "Use a Velo built-in lock handler or implement token-based reactive ownership.");
            }
            CompletionStage<LockToken> stage = ((ReactiveLockHandler) lockHandler).lockToken(key, wait, lease);
            // 通过独立的 guarded future 向 Reactor 交付：即使 Reactor 在取消时连带取消了 guarded，
            // 源 stage 的回调仍会在 token 产出时执行补偿，令牌不会丢失在窗口内。
            CompletableFuture<LockToken> guarded = new CompletableFuture<>();
            stage.whenComplete((token, error) -> {
                if (error != null) {
                    guarded.completeExceptionally(error);
                    return;
                }
                if (token != null) {
                    undelivered.set(token);
                    if (cancelled.get()) {
                        // 取消信号先于 token 产出到达：立即补偿释放
                        releaseUndelivered(undelivered);
                    }
                }
                guarded.complete(token);
            });
            return guarded;
        })
        .doOnCancel(() -> {
            cancelled.set(true);
            releaseUndelivered(undelivered);
        })
        // lockToken 返回 null 表示获取失败。Mono.fromCompletionStage 对 null 完成值发空信号，
        // 必须用 switchIfEmpty 转换为异常，否则锁超时会静默返回空响应。
        .switchIfEmpty(Mono.defer(
                () -> Mono.error(new LockException(resolveMessage(message), key, wait, lease))));
    }

    private void releaseUndelivered(AtomicReference<LockToken> undelivered) {
        LockToken orphan = undelivered.getAndSet(null);
        if (orphan != null) {
            // 与 usingWhen 的释放路径并发调用也无害：释放按 token 校验，重复调用是空操作。
            release(orphan).subscribe(null, error -> log.warn(
                    "[Velo Starter] Reactive lock compensation release failed for key '{}': {}",
                    orphan.getKey(), error.toString()));
        }
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
