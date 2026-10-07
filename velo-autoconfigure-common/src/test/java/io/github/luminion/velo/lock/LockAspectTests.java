package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.exception.LockException;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockAspectTests {

    @Test
    void shouldFallBackToMethodLevelLockWhenKeyExpressionIsBlank() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        LockAspect aspect = new LockAspect("lock:", new SpelFingerprinter(), capturingLockHandler(capturedKey));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new BlankKeyLockService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);

        BlankKeyLockService proxy = proxyFactory.getProxy();
        proxy.execute();

        // 未指定资源前缀与表达式时使用完整方法指纹。
        assertThat(capturedKey.get())
                .isEqualTo("lock:" + BlankKeyLockService.class.getName() + "#execute()");
    }

    @Test
    void nativeLockShouldBeReleasedAfterBusinessSuccessAndFailure() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        AspectJProxyFactory factory = new AspectJProxyFactory(new FailingLockService());
        factory.setProxyTargetClass(true);
        factory.addAspect(new LockAspect("lock:", new SpelFingerprinter(), handler));
        FailingLockService proxy = factory.getProxy();
        String key = "lock:" + FailingLockService.class.getName() + "#execute(boolean)";

        proxy.execute(false);
        assertReleased(handler, key);
        assertThatThrownBy(() -> proxy.execute(true))
                .isInstanceOf(IllegalStateException.class).hasMessage("business failed");
        assertReleased(handler, key);
    }

    private static void assertReleased(JdkLockHandler handler, String key) throws Exception {
        // 使用其他线程验证真正释放，避免本线程重入掩盖未解锁的问题。
        boolean acquired = CompletableFuture.supplyAsync(() -> {
            boolean locked = handler.tryLock(key);
            if (locked) {
                handler.unlock(key);
            }
            return locked;
        }).get(5, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
        assertThat((Map<?, ?>) ReflectionTestUtils.getField(handler, "lockMap")).isEmpty();
    }

    @Test
    void differentMethodsShouldContendForSameResourceAndAllowOtherResources() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        SharedLockService target = new SharedLockService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new LockAspect("lock:", new SpelFingerprinter(), handler));
        SharedLockService proxy = factory.getProxy();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> running = CompletableFuture.runAsync(() -> proxy.pay("one", () -> {
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test did not release lock");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }));
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> proxy.cancel("one")).isInstanceOf(LockException.class);
            proxy.cancel("two");
            assertThat(target.cancellations.get()).isEqualTo(1);
        } finally {
            release.countDown();
            running.get(5, TimeUnit.SECONDS);
        }
        proxy.cancel("one");
        assertThat(target.cancellations.get()).isEqualTo(2);
        assertReleased(handler, "lock:order:one");
    }

    @Test
    void returningPendingFutureShouldReleaseAtCurrentCallPoint() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        AsyncLockService target = new AsyncLockService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new LockAspect("lock:", new SpelFingerprinter(), handler));
        AsyncLockService proxy = factory.getProxy();
        CompletableFuture<Void> result = proxy.execute();
        assertThat(result).isSameAs(target.pending);
        assertThat(result.isDone()).isFalse();
        assertReleased(handler, "lock:async");
        result.completeExceptionally(new IllegalStateException("later failure"));
        assertReleased(handler, "lock:async");
    }

    private static LockHandler capturingLockHandler(AtomicReference<String> capturedKey) {
        return new LockHandler() {
            @Override
            public boolean tryLock(String key) {
                capturedKey.set(key);
                return true;
            }

            @Override
            public void unlock(String key) {
            }
        };
    }

    static class BlankKeyLockService {
        @Lock
        public void execute() {
        }
    }

    static class FailingLockService {
        @Lock
        public void execute(boolean fail) {
            if (fail) {
                throw new IllegalStateException("business failed");
            }
        }
    }

    static class SharedLockService {
        private final AtomicInteger cancellations = new AtomicInteger();

        @Lock(prefix = "order", value = "#p0")
        public void pay(String id, Runnable action) {
            action.run();
        }

        @Lock(prefix = "order", value = "#p0")
        public void cancel(String id) {
            cancellations.incrementAndGet();
        }
    }

    static class AsyncLockService {
        private final CompletableFuture<Void> pending = new CompletableFuture<>();

        @Lock(prefix = "async")
        public CompletableFuture<Void> execute() {
            return pending;
        }
    }
}
