package io.github.luminion.velo.lock;

import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockAspectTests {

    @Test
    void shouldRejectBlankLockKeyExpression() {
        LockAspect aspect = new LockAspect("lock:", constantFingerprinter(), noopLockHandler());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new BlankKeyLockService());
        proxyFactory.addAspect(aspect);

        BlankKeyLockService proxy = proxyFactory.getProxy();

        assertThatThrownBy(proxy::execute)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lock key");
    }

    private static Fingerprinter constantFingerprinter() {
        return (target, method, args, expression) -> "ignored";
    }

    private static LockHandler noopLockHandler() {
        return new LockHandler() {
            @Override
            public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
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
