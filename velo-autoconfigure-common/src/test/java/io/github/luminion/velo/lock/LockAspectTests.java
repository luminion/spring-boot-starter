package io.github.luminion.velo.lock;

import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

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
                .isEqualTo("lock:" + BlankKeyLockService.class.getName() + "#execute");
    }

    private static LockHandler capturingLockHandler(AtomicReference<String> capturedKey) {
        return new LockHandler() {
            @Override
            public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
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
}
