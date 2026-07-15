package io.github.luminion.velo.idempotent;

import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotentAspectTests {

    @Test
    void shouldUseMethodFingerprintWhenKeyExpressionIsBlank() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        IdempotentAspect aspect = new IdempotentAspect("idempotent:", new SpelFingerprinter(), new IdempotentHandler() {
            @Override
            public boolean tryRecord(String key, String token, long timeout) {
                capturedKey.set(key);
                return true;
            }
        });
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DefaultKeyIdempotentService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);

        DefaultKeyIdempotentService proxy = proxyFactory.getProxy();
        proxy.submit("u-1001");

        assertThat(capturedKey.get()).isEqualTo("idempotent:" + DefaultKeyIdempotentService.class.getName()
                + "#submit(java.lang.String)");
    }

    @Test
    void shouldPrefixExplicitKeyWithMethodFingerprint() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        IdempotentAspect aspect = new IdempotentAspect("idempotent:", new SpelFingerprinter(), new IdempotentHandler() {
            @Override
            public boolean tryRecord(String key, String token, long timeout) {
                capturedKey.set(key);
                return true;
            }
        });
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new ExplicitKeyIdempotentService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);

        ExplicitKeyIdempotentService proxy = proxyFactory.getProxy();
        proxy.submit("order-1");

        // 显式 key 也带方法指纹前缀：idempotent:类名#方法名:SpEL结果
        assertThat(capturedKey.get())
                .isEqualTo("idempotent:" + ExplicitKeyIdempotentService.class.getName()
                        + "#submit(java.lang.String):order-1");
    }

    @Test
    void shouldNotCollideAcrossDifferentMethodsWithSameKeyValue() {
        AtomicReference<String> keyA = new AtomicReference<>();
        AtomicReference<String> keyB = new AtomicReference<>();
        IdempotentAspect aspect = new IdempotentAspect("idempotent:", new SpelFingerprinter(), new IdempotentHandler() {
            @Override
            public boolean tryRecord(String key, String token, long timeout) {
                // 两个方法各记录一次自己的 key，用于比对是否碰撞
                if (keyA.get() == null) {
                    keyA.set(key);
                } else {
                    keyB.set(key);
                }
                return true;
            }
        });
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TwoMethodIdempotentService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);

        TwoMethodIdempotentService proxy = proxyFactory.getProxy();
        proxy.methodA("same-id");
        proxy.methodB("same-id");

        // 相同 SpEL 结果，但不同方法，key 必须不同（不共享幂等窗口）
        assertThat(keyA.get()).isNotEqualTo(keyB.get());
        assertThat(keyA.get()).contains("#methodA(java.lang.String):same-id");
        assertThat(keyB.get()).contains("#methodB(java.lang.String):same-id");
    }

    static class DefaultKeyIdempotentService {

        @Idempotent
        public void submit(String userId) {
        }
    }

    static class ExplicitKeyIdempotentService {

        @Idempotent(key = "#p0")
        public void submit(String orderId) {
        }
    }

    static class TwoMethodIdempotentService {

        @Idempotent(key = "#p0")
        public void methodA(String id) {
        }

        @Idempotent(key = "#p0")
        public void methodB(String id) {
        }
    }
}
