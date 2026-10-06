package io.github.luminion.velo.lock;

import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

        // 空 key 降级为方法级锁（类名#方法名）
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
}
