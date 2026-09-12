package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.ReactiveTypeSupport;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.exception.IdempotentException;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * WebFlux 响应式幂等切面。
 *
 * <p>幂等记录在订阅时创建，响应式链异常或取消时清理；正常完成则保留至注解 TTL，
 * 继续保持“TTL 窗口内拒绝重复提交”的语义。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Aspect
public class WebFluxIdempotentAspect implements Ordered {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebFluxIdempotentAspect.class);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final IdempotentHandler idempotentHandler;
    private final VeloMessageResolver messageResolver;

    private int order = VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT;

    public WebFluxIdempotentAspect(String prefix, Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
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
        if (!ReactiveTypeSupport.isReactiveType(signature.getReturnType())) {
            return joinPoint.proceed();
        }

        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());
        long ttl = idempotent.ttl();
        if (ttl <= 0L) {
            throw new IllegalArgumentException("Idempotent ttl must be greater than zero.");
        }

        if (!StringUtils.hasText(idempotent.key())) {
            log.warn("[Velo Starter] @Idempotent on {}#{} has no 'key' expression. " +
                            "It will fall back to method-level idempotency, meaning all invocations of this method " +
                            "(regardless of arguments or caller) share a single idempotency window. " +
                            "Specify a SpEL key (e.g. key=\"#userId\") unless this is intended.",
                    method.getDeclaringClass().getName(), method.getName());
        }

        String methodFingerprint = fingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(), method, joinPoint.getArgs(), "");
        String keyFingerprint = methodFingerprint;
        if (StringUtils.hasText(idempotent.key())) {
            keyFingerprint += ':' + fingerprinter.resolveMethodFingerprint(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), idempotent.key());
        }
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);
        if (WebFluxReactiveSupport.isMonoReturnType(signature.getReturnType())) {
            return applyMono(joinPoint, idempotent, key, ttl);
        }
        return applyFlux(joinPoint, idempotent, key, ttl);
    }

    private Mono<?> applyMono(ProceedingJoinPoint joinPoint, Idempotent idempotent, String key, long ttl) {
        return Mono.defer(() -> {
            String token = UUID.randomUUID().toString();
            return WebFluxReactiveSupport.blocking(() -> idempotentHandler.tryRecord(key, token, ttl))
                .flatMap(accepted -> {
                    if (!accepted) {
                        return Mono.error(new IdempotentException(resolveMessage(idempotent.message()), key, ttl));
                    }
                    return Mono.usingWhen(
                            Mono.just(token),
                            ignored -> WebFluxReactiveSupport.proceedMono(joinPoint),
                            ignored -> Mono.empty(),
                            (ignored, error) -> cleanup(key, token),
                            ignored -> cleanup(key, token));
                });
        });
    }

    private Flux<?> applyFlux(ProceedingJoinPoint joinPoint, Idempotent idempotent, String key, long ttl) {
        return Flux.defer(() -> {
            String token = UUID.randomUUID().toString();
            return WebFluxReactiveSupport.blocking(() -> idempotentHandler.tryRecord(key, token, ttl))
                    .flatMapMany(accepted -> {
                    if (!accepted) {
                        return Flux.error(new IdempotentException(resolveMessage(idempotent.message()), key, ttl));
                    }
                    return Flux.usingWhen(
                            Mono.just(token),
                            ignored -> WebFluxReactiveSupport.proceedFlux(joinPoint),
                            ignored -> Mono.empty(),
                            (ignored, error) -> cleanup(key, token),
                            ignored -> cleanup(key, token));
                    });
        });
    }

    private Mono<Void> cleanup(String key, String token) {
        return WebFluxReactiveSupport.blockingRun(() -> {
            try {
                idempotentHandler.removeIfMatch(key, token);
            } catch (Throwable cleanupEx) {
                log.warn("[Velo Starter] Reactive idempotent cleanup failed for key '{}': {}", key,
                        cleanupEx.toString());
            }
        });
    }

    private String resolveMessage(String message) {
        return messageResolver != null ? messageResolver.resolve(message) : message;
    }
}
