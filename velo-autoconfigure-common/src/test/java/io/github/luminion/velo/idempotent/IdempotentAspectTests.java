package io.github.luminion.velo.idempotent;

import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotentAspectTests {

    @Test
    void shouldUseMethodFingerprintWhenKeyExpressionIsBlank() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        IdempotentAspect aspect = new IdempotentAspect("idempotent:", new SpelFingerprinter(), new IdempotentHandler() {
            @Override
            public boolean tryLock(String key, long timeout, TimeUnit unit) {
                capturedKey.set(key);
                return true;
            }

            @Override
            public void unlock(String key) {
            }
        });
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DefaultKeyIdempotentService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);

        DefaultKeyIdempotentService proxy = proxyFactory.getProxy();
        proxy.submit("u-1001");

        assertThat(capturedKey.get()).isEqualTo("idempotent:" + DefaultKeyIdempotentService.class.getName() + "#submit");
    }

    static class DefaultKeyIdempotentService {

        @Idempotent
        public void submit(String userId) {
        }
    }
}
