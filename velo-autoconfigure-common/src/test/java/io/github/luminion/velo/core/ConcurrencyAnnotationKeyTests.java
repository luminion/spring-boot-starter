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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcurrencyAnnotationKeyTests {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldApplyShorthandAndNamedExpressionsThroughBothProxyTypes(boolean classProxy) {
        KeyService target = new KeyService();
        List<String> calls = new ArrayList<>();
        Operations proxy = createProxy(target, classProxy, calls);
        proxy.shorthand("id");
        proxy.named("id");
        assertThat(target.executions).isEqualTo(2);
        assertThat(calls).hasSize(6);
        for (String method : new String[]{"shorthand", "named"}) {
            String key = KeyService.class.getName() + '#' + method + "(java.lang.String):id";
            assertThat(calls).contains("lock:" + key, "idempotent:" + key, "rate:" + key + ":7");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitPrefixesShouldShareAcrossClassesAndKeepFeaturesSeparate(boolean classProxy) {
        List<String> calls = new ArrayList<>();
        SharedOperations first = createProxy(new FirstSharedService(), classProxy, calls);
        SharedOperations second = createProxy(new SecondSharedService(), classProxy, calls);
        first.execute("one");
        second.execute("one");
        second.execute("two");
        assertThat(calls).containsExactly(
                "idempotent:order:one", "rate:order:one:7", "lock:order:one",
                "idempotent:order:one", "rate:order:one:7", "lock:order:one",
                "idempotent:order:two", "rate:order:two:7", "lock:order:two");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void prefixWithoutExpressionShouldIgnoreArguments(boolean classProxy) {
        List<String> calls = new ArrayList<>();
        Operations proxy = createProxy(new KeyService(), classProxy, calls);
        proxy.global("one");
        proxy.global("two");
        assertThat(calls).containsExactly(
                "idempotent:all", "rate:all:7", "lock:all",
                "idempotent:all", "rate:all:7", "lock:all");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidAttributesShouldFailBeforeHandlerOrBusiness(boolean classProxy) {
        KeyService target = new KeyService();
        List<String> calls = new ArrayList<>();
        Operations proxy = createProxy(target, classProxy, calls);
        assertThatThrownBy(() -> proxy.invalidQps("id")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qps");
        assertThatThrownBy(() -> proxy.invalidTtl("id")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl");
        assertThatThrownBy(() -> proxy.invalidPrefix("id")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prefix");
        assertThat(calls).isEmpty();
        assertThat(target.executions).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void classRateAndMethodOverrideShouldEachApplyOnce(boolean classProxy) {
        List<String> calls = new ArrayList<>();
        ClassRateOperations proxy = createProxy(new ClassRateService(), classProxy, calls);
        proxy.defaultRate("id");
        proxy.otherDefaultRate("id");
        proxy.overrideRate("id");
        assertThat(calls).containsExactly("rate:class:id:9", "rate:class:id:9", "rate:method:id:3");
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
        void shorthand(String id);
        void named(String id);
        void global(String id);
        void invalidQps(String id);
        void invalidTtl(String id);
        void invalidPrefix(String id);
    }

    static class KeyService implements Operations {
        private int executions;

        @Override
        @Lock("#p0")
        @Idempotent("#p0")
        @RateLimit(value = "#p0", qps = 7)
        public void shorthand(String id) { executions++; }

        @Override
        @Lock(value = "#p0")
        @Idempotent(value = "#p0")
        @RateLimit(value = "#p0", qps = 7)
        public void named(String id) { executions++; }

        @Override
        @Lock(prefix = "all")
        @Idempotent(prefix = "all")
        @RateLimit(prefix = "all", qps = 7)
        public void global(String id) { executions++; }

        @Override
        @RateLimit(qps = 0)
        public void invalidQps(String id) { executions++; }

        @Override
        @Idempotent(ttl = 0)
        public void invalidTtl(String id) { executions++; }

        @Override
        @Lock(prefix = ":::")
        public void invalidPrefix(String id) { executions++; }
    }

    interface SharedOperations {
        void execute(String id);
    }

    static class FirstSharedService implements SharedOperations {
        @Override
        @Lock(prefix = "order", value = "#p0")
        @Idempotent(prefix = "order", value = "#p0")
        @RateLimit(prefix = "order", value = "#p0", qps = 7)
        public void execute(String id) {
        }
    }

    static class SecondSharedService implements SharedOperations {
        @Override
        @Lock(prefix = "order", value = "#p0")
        @Idempotent(prefix = "order", value = "#p0")
        @RateLimit(prefix = "order", value = "#p0", qps = 7)
        public void execute(String id) {
        }
    }

    interface ClassRateOperations {
        void defaultRate(String id);
        void otherDefaultRate(String id);
        void overrideRate(String id);
    }

    @RateLimit(prefix = "class", value = "#p0", qps = 9)
    static class ClassRateService implements ClassRateOperations {
        @Override
        public void defaultRate(String id) {
        }

        @Override
        public void otherDefaultRate(String id) {
        }

        @Override
        @RateLimit(prefix = "method", value = "#p0", qps = 3)
        public void overrideRate(String id) {
        }
    }
}
