package io.github.luminion.velo.webflux;

import org.aspectj.lang.ProceedingJoinPoint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.reactivestreams.Publisher;

import java.util.concurrent.Callable;

/**
 * WebFlux 响应式切面的公共工具。
 *
 * @author luminion
 * @since 1.3.1
 */
final class WebFluxReactiveSupport {

    private static final Scheduler BLOCKING_SCHEDULER = Schedulers.boundedElastic();

    private WebFluxReactiveSupport() {
    }

    static boolean isMonoReturnType(Class<?> returnType) {
        return returnType != null && Mono.class.isAssignableFrom(returnType);
    }

    static <T> Mono<T> blocking(Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(BLOCKING_SCHEDULER);
    }

    static <T> Mono<T> fromStage(Callable<java.util.concurrent.CompletionStage<T>> callable) {
        return blocking(callable).flatMap(Mono::fromCompletionStage);
    }

    static Mono<Void> blockingRun(Runnable runnable) {
        return Mono.<Void>fromRunnable(runnable).subscribeOn(BLOCKING_SCHEDULER);
    }

    static Mono<?> proceedMono(ProceedingJoinPoint joinPoint) {
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Mono<?>) {
                return (Mono<?>) result;
            }
            if (result instanceof Publisher<?>) {
                return Mono.from((Publisher<?>) result);
            }
            return Mono.error(new IllegalStateException("Reactive method must return a Publisher."));
        } catch (Throwable ex) {
            return Mono.error(ex);
        }
    }

    static Flux<?> proceedFlux(ProceedingJoinPoint joinPoint) {
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Flux<?>) {
                return (Flux<?>) result;
            }
            if (result instanceof Publisher<?>) {
                return Flux.from((Publisher<?>) result);
            }
            return Flux.error(new IllegalStateException("Reactive method must return a Publisher."));
        } catch (Throwable ex) {
            return Flux.error(ex);
        }
    }
}
