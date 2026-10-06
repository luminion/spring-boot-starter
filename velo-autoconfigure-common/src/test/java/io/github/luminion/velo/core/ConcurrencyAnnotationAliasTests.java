package io.github.luminion.velo.core;

import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.core.annotation.AnnotationConfigurationException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcurrencyAnnotationAliasTests {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldApplyValueNamedAndEqualAliasesThroughBothProxyTypes(boolean classProxy) {
        AliasService target = new AliasService();
        List<String> calls = new ArrayList<>();
        Operations proxy = createProxy(target, classProxy, calls);
        proxy.value("id");
        proxy.named("id");
        proxy.equal("id");
        assertThat(target.executions).isEqualTo(3);
        assertThat(calls).hasSize(9);
        for (String method : new String[]{"value", "named", "equal"}) {
            String key = AliasService.class.getName() + '#' + method + "(java.lang.String):id";
            assertThat(calls).contains("lock:" + key, "idempotent:" + key, "rate:" + key + ":7");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void conflictingAliasesShouldFailBeforeHandlerOrBusiness(boolean classProxy) {
        AliasService target = new AliasService();
        List<String> calls = new ArrayList<>();
        Operations proxy = createProxy(target, classProxy, calls);
        assertThatThrownBy(() -> proxy.lockConflict("id")).isInstanceOf(AnnotationConfigurationException.class);
        assertThatThrownBy(() -> proxy.idempotentConflict("id")).isInstanceOf(AnnotationConfigurationException.class);
        assertThatThrownBy(() -> proxy.rateConflict("id")).isInstanceOf(AnnotationConfigurationException.class);
        assertThat(calls).isEmpty();
        assertThat(target.executions).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidQpsShouldFailBeforeHandlerOrBusiness(boolean classProxy) {
        AliasService target = new AliasService();
        List<String> calls = new ArrayList<>();
        Operations proxy = createProxy(target, classProxy, calls);
        assertThatThrownBy(() -> proxy.invalid("id")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qps");
        assertThat(calls).isEmpty();
        assertThat(target.executions).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void classAliasAndMethodOverrideShouldEachApplyOnce(boolean classProxy) {
        List<String> calls = new ArrayList<>();
        ClassRateOperations proxy = createProxy(new ClassRateService(), classProxy, calls);
        proxy.defaultRate("id");
        proxy.overrideRate("id");
        assertThat(calls).containsExactly(
                "rate:" + ClassRateService.class.getName() + "#defaultRate(java.lang.String):id:9",
                "rate:" + ClassRateService.class.getName() + "#overrideRate(java.lang.String):id:3");
    }

    private static <T> T createProxy(Object target, boolean classProxy, List<String> calls) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(classProxy);
        SpelFingerprinter fingerprinter = new SpelFingerprinter();
        factory.addAspect(new IdempotentAspect("idempotent:", fingerprinter, (key, token, ttl) -> {
            calls.add(key);
            return true;
        }));
        factory.addAspect(new RateLimitAspect("rate:", fingerprinter, (key, qps) -> {
            calls.add(key + ':' + qps);
            return true;
        }));
        factory.addAspect(new LockAspect("lock:", fingerprinter, new LockHandler() {
            @Override
            public boolean tryLock(String key) {
                calls.add(key);
                return true;
            }

            @Override
            public void unlock(String key) {
            }
        }));
        return factory.getProxy();
    }

    interface Operations {
        void value(String id);
        void named(String id);
        void equal(String id);
        void lockConflict(String id);
        void idempotentConflict(String id);
        void rateConflict(String id);
        void invalid(String id);
    }

    static class AliasService implements Operations {
        private int executions;

        @Override
        @Lock("#p0")
        @Idempotent("#p0")
        @RateLimit(value = 7, key = "#p0")
        public void value(String id) { executions++; }

        @Override
        @Lock(key = "#p0")
        @Idempotent(key = "#p0")
        @RateLimit(qps = 7, key = "#p0")
        public void named(String id) { executions++; }

        @Override
        @Lock(value = "#p0", key = "#p0")
        @Idempotent(value = "#p0", key = "#p0")
        @RateLimit(value = 7, qps = 7, key = "#p0")
        public void equal(String id) { executions++; }

        @Override
        @Lock(value = "#p0", key = "'other'")
        public void lockConflict(String id) { executions++; }

        @Override
        @Idempotent(value = "#p0", key = "'other'")
        public void idempotentConflict(String id) { executions++; }

        @Override
        @RateLimit(value = 7, qps = 8)
        public void rateConflict(String id) { executions++; }

        @Override
        @RateLimit(0)
        public void invalid(String id) { executions++; }
    }

    interface ClassRateOperations {
        void defaultRate(String id);
        void overrideRate(String id);
    }

    @RateLimit(value = 9, key = "#p0")
    static class ClassRateService implements ClassRateOperations {
        @Override
        public void defaultRate(String id) {
        }

        @Override
        @RateLimit(value = 3, key = "#p0")
        public void overrideRate(String id) {
        }
    }
}
