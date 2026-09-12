package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.exception.IdempotentException;
import io.github.luminion.velo.lock.LockToken;
import io.github.luminion.velo.lock.ReactiveLockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WebFlux 通用注解切面的订阅生命周期测试。
 */
class WebFluxReactiveAspectTests {

    @Test
    void shouldRecordIdempotencyAtSubscriptionAndKeepSuccessfulRecordUntilTtl() {
        RecordingIdempotentHandler handler = new RecordingIdempotentHandler();
        IdempotentService proxy = proxy(new IdempotentService(),
                new WebFluxIdempotentAspect("idempotent:", new SpelFingerprinter(), handler, null));

        Mono<?> result = proxy.success("order-1");

        assertThat(handler.recordCount.get()).isZero();
        assertThat(result.block()).isEqualTo("ok");
        assertThat(handler.recordCount.get()).isEqualTo(1);
        assertThat(handler.records).hasSize(1);
        assertThat(handler.removeCount.get()).isZero();
    }

    @Test
    void shouldRemoveIdempotencyRecordWhenReactiveChainFails() {
        RecordingIdempotentHandler handler = new RecordingIdempotentHandler();
        IdempotentService proxy = proxy(new IdempotentService(),
                new WebFluxIdempotentAspect("idempotent:", new SpelFingerprinter(), handler, null));

        assertThatThrownBy(() -> proxy.failure().block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business failure");

        awaitUntil(() -> handler.records.isEmpty());
        assertThat(handler.removeCount.get()).isEqualTo(1);
    }

    @Test
    void shouldRemoveIdempotencyRecordWhenSubscriptionIsCancelled() throws Exception {
        RecordingIdempotentHandler handler = new RecordingIdempotentHandler();
        IdempotentService proxy = proxy(new IdempotentService(),
                new WebFluxIdempotentAspect("idempotent:", new SpelFingerprinter(), handler, null));

        Disposable subscription = proxy.never().subscribe();
        assertThat(handler.recorded.await(2, TimeUnit.SECONDS)).isTrue();
        subscription.dispose();

        awaitUntil(() -> handler.records.isEmpty());
        assertThat(handler.removeCount.get()).isEqualTo(1);
    }

    @Test
    void shouldAcquireAndReleaseReactiveLockAroundActualSubscription() {
        RecordingReactiveLockHandler handler = new RecordingReactiveLockHandler();
        LockService proxy = proxy(new LockService(),
                new WebFluxLockAspect("lock:", new SpelFingerprinter(), handler, null));

        Mono<?> result = proxy.success("order-1");

        assertThat(handler.acquireCount.get()).isZero();
        assertThat(result.block()).isEqualTo("ok");
        assertThat(handler.acquireCount.get()).isEqualTo(1);
        assertThat(handler.releaseCount.get()).isEqualTo(1);
    }

    @Test
    void shouldReleaseReactiveLockAfterErrorAndCancellation() throws Exception {
        RecordingReactiveLockHandler handler = new RecordingReactiveLockHandler();
        LockService proxy = proxy(new LockService(),
                new WebFluxLockAspect("lock:", new SpelFingerprinter(), handler, null));

        assertThatThrownBy(() -> proxy.failure().block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business failure");
        awaitUntil(() -> handler.releaseCount.get() == 1);

        handler.reset();
        Disposable subscription = proxy.never().subscribe();
        assertThat(handler.acquired.await(2, TimeUnit.SECONDS)).isTrue();
        subscription.dispose();

        awaitUntil(() -> handler.releaseCount.get() == 1);
    }

    @Test
    void shouldApplyRateLimitOnlyWhenPublisherIsSubscribed() {
        RecordingRateLimitHandler handler = new RecordingRateLimitHandler(true);
        RateLimitService proxy = proxy(new RateLimitService(),
                new WebFluxRateLimitAspect("rate:", new SpelFingerprinter(), handler, null));

        Mono<?> result = proxy.success();

        assertThat(handler.acquireCount.get()).isZero();
        assertThat(result.block()).isEqualTo("ok");
        assertThat(handler.acquireCount.get()).isEqualTo(1);

        handler.allowed = false;
        assertThatThrownBy(() -> proxy.success().block())
                .isInstanceOf(io.github.luminion.velo.ratelimit.exception.RateLimitException.class);
        assertThat(handler.acquireCount.get()).isEqualTo(2);
    }

    @Test
    void shouldWriteInvokeLogsAfterReactiveSignals() {
        CapturingWriter writer = new CapturingWriter();
        VeloProperties properties = new VeloProperties();
        RuntimeJsonSerializer serializer = value -> "json";
        WebFluxInvokeLogAspect aspect = new WebFluxInvokeLogAspect(properties, provider(serializer), writer);
        LogService proxy = proxy(new LogService(), aspect);

        Mono<?> mono = proxy.invokeMono("value");

        assertThat(writer.records).isEmpty();
        assertThat(mono.block()).isEqualTo("ok");
        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(0).getPhase()).isEqualTo(InvocationPhase.ENTRY);
        assertThat(writer.records.get(1).getPhase()).isEqualTo(InvocationPhase.EXIT);
        assertThat(writer.records.get(1).isSuccess()).isTrue();
        assertThat(writer.records.get(1).getResult()).isEqualTo("json");
    }

    @Test
    void shouldWriteFluxInvokeLogWithElementCount() {
        CapturingWriter writer = new CapturingWriter();
        VeloProperties properties = new VeloProperties();
        WebFluxInvokeLogAspect aspect = new WebFluxInvokeLogAspect(properties, provider(value -> "json"), writer);
        LogService proxy = proxy(new LogService(), aspect);

        proxy.invokeFlux().blockLast();

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(0).getPhase()).isEqualTo(InvocationPhase.ENTRY);
        assertThat(writer.records.get(1).getResult()).isEqualTo("Flux{count=2}");
    }

    @Test
    void shouldMeasureReactiveSlowLogUntilCompletion() {
        CapturingWriter writer = new CapturingWriter();
        VeloProperties properties = new VeloProperties();
        WebFluxSlowLogAspect aspect = new WebFluxSlowLogAspect(properties, provider(value -> "json"), writer);
        LogService proxy = proxy(new LogService(), aspect);

        proxy.slowMono().block();

        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).isSlow()).isTrue();
        assertThat(writer.records.get(0).getPhase()).isNull();
        assertThat(writer.records.get(0).getCostMs()).isGreaterThanOrEqualTo(1L);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(T target, Object... aspects) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        for (Object aspect : aspects) {
            proxyFactory.addAspect(aspect);
        }
        return (T) proxyFactory.getProxy();
    }

    private static ObjectProvider<RuntimeJsonSerializer> provider(RuntimeJsonSerializer serializer) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("runtimeJsonSerializer", serializer);
        return beanFactory.getBeanProvider(RuntimeJsonSerializer.class);
    }

    private static void awaitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待响应式清理完成时被中断", ex);
            }
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    static class IdempotentService {

        @Idempotent(key = "#p0")
        public Mono<String> success(String orderId) {
            return Mono.just("ok");
        }

        @Idempotent(key = "'failure'")
        public Mono<String> failure() {
            return Mono.error(new IllegalStateException("business failure"));
        }

        @Idempotent(key = "'never'")
        public Mono<String> never() {
            return Mono.never();
        }
    }

    static class LockService {

        @Lock(key = "#p0")
        public Mono<String> success(String orderId) {
            return Mono.just("ok");
        }

        @Lock(key = "failure")
        public Mono<String> failure() {
            return Mono.error(new IllegalStateException("business failure"));
        }

        @Lock(key = "never")
        public Mono<String> never() {
            return Mono.never();
        }
    }

    static class RateLimitService {

        @RateLimit(permits = 1, window = 1000)
        public Mono<String> success() {
            return Mono.just("ok");
        }
    }

    static class LogService {

        @InvokeLog(argsOnFinish = true)
        public Mono<String> invokeMono(String value) {
            return Mono.just("ok").delayElement(Duration.ofMillis(5));
        }

        @InvokeLog
        public Flux<String> invokeFlux() {
            return Flux.just("a", "b");
        }

        @SlowLog(1)
        public Mono<String> slowMono() {
            return Mono.just("ok").delayElement(Duration.ofMillis(15));
        }
    }

    static final class RecordingIdempotentHandler implements IdempotentHandler {

        private final Map<String, String> records = new ConcurrentHashMap<>();
        private final AtomicInteger recordCount = new AtomicInteger();
        private final AtomicInteger removeCount = new AtomicInteger();
        private final CountDownLatch recorded = new CountDownLatch(1);

        @Override
        public boolean tryRecord(String key, String token, long timeout) {
            recordCount.incrementAndGet();
            recorded.countDown();
            return records.putIfAbsent(key, token) == null;
        }

        @Override
        public void removeIfMatch(String key, String token) {
            records.computeIfPresent(key, (ignored, current) -> {
                if (current.equals(token)) {
                    removeCount.incrementAndGet();
                    return null;
                }
                return current;
            });
        }
    }

    static final class RecordingReactiveLockHandler implements ReactiveLockHandler {

        private final AtomicInteger acquireCount = new AtomicInteger();
        private final AtomicInteger releaseCount = new AtomicInteger();
        private volatile CountDownLatch acquired = new CountDownLatch(1);

        @Override
        public boolean lock(String key, long waitTime, long leaseTime) {
            return true;
        }

        @Override
        public void unlock(String key) {
        }

        @Override
        public CompletableFuture<LockToken> lockToken(String key, long waitTime, long leaseTime) {
            acquireCount.incrementAndGet();
            acquired.countDown();
            return CompletableFuture.completedFuture(new LockToken(key, "owner" + acquireCount.get()));
        }

        @Override
        public CompletableFuture<Void> unlockToken(LockToken token) {
            releaseCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        private void reset() {
            acquireCount.set(0);
            releaseCount.set(0);
            acquired = new CountDownLatch(1);
        }
    }

    static final class RecordingRateLimitHandler implements RateLimitHandler {

        private final AtomicInteger acquireCount = new AtomicInteger();
        private volatile boolean allowed;

        private RecordingRateLimitHandler(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean tryAcquire(String key, double rate, long window) {
            acquireCount.incrementAndGet();
            return allowed;
        }
    }

    static final class CapturingWriter implements InvocationLogWriter {

        private final java.util.List<InvocationLogRecord> records = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void write(InvocationLogRecord record) {
            records.add(record);
        }
    }
}
