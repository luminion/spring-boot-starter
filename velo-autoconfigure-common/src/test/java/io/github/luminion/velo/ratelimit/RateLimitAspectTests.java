package io.github.luminion.velo.ratelimit;

import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitAspectTests {

    @Test
    void shouldResolveClassLevelAnnotationFromTargetClassForInterfaceProxy() {
        AtomicReference<Method> resolvedMethod = new AtomicReference<>();
        AtomicReference<String> resolvedKey = new AtomicReference<>();

        Fingerprinter fingerprinter = (target, method, args, expression) -> {
            resolvedMethod.set(method);
            return "tenant:" + args[0];
        };
        RateLimitHandler handler = (key, rate, timeout, unit) -> {
            resolvedKey.set(key);
            return true;
        };
        RateLimitAspect aspect = new RateLimitAspect("rateLimit:", fingerprinter, handler);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new ClassLevelRateLimitedService());
        proxyFactory.setInterfaces(SampleService.class);
        proxyFactory.setProxyTargetClass(false);
        proxyFactory.addAspect(aspect);

        SampleService proxy = proxyFactory.getProxy();
        proxy.execute("u-1001");

        assertThat(resolvedMethod.get()).isNotNull();
        assertThat(resolvedMethod.get().getDeclaringClass()).isEqualTo(ClassLevelRateLimitedService.class);
        assertThat(resolvedKey.get()).isEqualTo("rateLimit:tenant:u-1001");
    }

    interface SampleService {
        void execute(String userId);
    }

    @RateLimit(key = "#p0", permits = 5, ttl = 1, unit = TimeUnit.SECONDS)
    static class ClassLevelRateLimitedService implements SampleService {
        @Override
        public void execute(String userId) {
        }
    }
}
