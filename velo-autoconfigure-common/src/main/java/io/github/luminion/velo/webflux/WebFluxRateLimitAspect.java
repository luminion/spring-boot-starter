package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.ReactiveTypeSupport;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
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

/**
 * WebFlux 响应式限流切面。
 *
 * <p>限流检查在订阅时执行，并将同步处理器放到 bounded-elastic 调度器，避免 Redis 等
 * 阻塞式实现阻塞 WebFlux 事件循环。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Aspect
public class WebFluxRateLimitAspect implements Ordered {

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final RateLimitHandler rateLimitHandler;
    private final VeloMessageResolver messageResolver;

    private int order = VeloAdvisorOrder.CONCURRENCY_RATE_LIMIT;

    public WebFluxRateLimitAspect(String prefix, Fingerprinter fingerprinter, RateLimitHandler rateLimitHandler,
            VeloMessageResolver messageResolver) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.rateLimitHandler = rateLimitHandler;
        this.messageResolver = messageResolver;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return applyRateLimit(joinPoint, rateLimit);
    }

    @Around("!@annotation(io.github.luminion.velo.ratelimit.annotation.RateLimit) && @within(rateLimit)")
    public Object doRateLimitOnClass(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return applyRateLimit(joinPoint, rateLimit);
    }

    private Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!ReactiveTypeSupport.isReactiveType(signature.getReturnType())) {
            return joinPoint.proceed();
        }

        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());
        String methodFingerprint = fingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(), method, joinPoint.getArgs(), "");
        String keyFingerprint = methodFingerprint;
        if (StringUtils.hasText(rateLimit.key())) {
            keyFingerprint += ':' + fingerprinter.resolveMethodFingerprint(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), rateLimit.key());
        }
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        if (WebFluxReactiveSupport.isMonoReturnType(signature.getReturnType())) {
            return Mono.defer(() -> check(key, rateLimit))
                    .flatMap(allowed -> allowed
                            ? WebFluxReactiveSupport.proceedMono(joinPoint)
                            : Mono.error(new RateLimitException(resolveMessage(rateLimit.message()), key,
                                    rateLimit.permits(), rateLimit.window())));
        }
        return Flux.defer(() -> check(key, rateLimit)
                .flatMapMany(allowed -> allowed
                        ? WebFluxReactiveSupport.proceedFlux(joinPoint)
                        : Flux.error(new RateLimitException(resolveMessage(rateLimit.message()), key,
                                rateLimit.permits(), rateLimit.window()))));
    }

    private Mono<Boolean> check(String key, RateLimit rateLimit) {
        return WebFluxReactiveSupport.blocking(() -> rateLimitHandler.tryAcquire(
                key, rateLimit.permits(), rateLimit.window()));
    }

    private String resolveMessage(String message) {
        return messageResolver != null ? messageResolver.resolve(message) : message;
    }
}
