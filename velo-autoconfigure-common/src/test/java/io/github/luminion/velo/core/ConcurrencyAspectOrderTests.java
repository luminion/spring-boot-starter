package io.github.luminion.velo.core;

import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcurrencyAspectOrderTests {

    @Test
    void shouldApplyStarterConcurrencyAspectsInDocumentedOrder() {
        List<String> events = new ArrayList<>();
        CombinedConcurrencyService proxy = createProxy(events, true);

        proxy.submit("order-1001");

        assertThat(events).containsExactly(
                "idempotent",
                "rateLimit",
                "lock",
                "business",
                "unlock");
    }

    @Test
    void shouldNotAcquireLockWhenRateLimitRejectsAfterIdempotentAccepted() {
        List<String> events = new ArrayList<>();
        CombinedConcurrencyService proxy = createProxy(events, false);

        assertThatThrownBy(() -> proxy.submit("order-1001"))
                .isInstanceOf(RateLimitException.class);

        assertThat(events).containsExactly("idempotent", "rateLimit");
    }

    private static CombinedConcurrencyService createProxy(List<String> events, boolean rateLimitAccepted) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new CombinedConcurrencyService(events));
        proxyFactory.setProxyTargetClass(true);
        // Add aspects in @Order ascending sequence (lowest precedence value = outermost layer).
        // AspectJProxyFactory chains advisors in add-order, so the first added wraps the rest.
        proxyFactory.addAspect(new IdempotentAspect("idempotent:", new SpelFingerprinter(), (key, timeout, unit) -> {
            events.add("idempotent");
            return true;
        }));
        proxyFactory.addAspect(new RateLimitAspect("rateLimit:", new SpelFingerprinter(), (key, rate, timeout, unit) -> {
            events.add("rateLimit");
            return rateLimitAccepted;
        }));
        proxyFactory.addAspect(new LockAspect("lock:", new SpelFingerprinter(), new LockHandler() {
            @Override
            public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
                events.add("lock");
                return true;
            }

            @Override
            public void unlock(String key) {
                events.add("unlock");
            }
        }));
        return proxyFactory.getProxy();
    }

    static class CombinedConcurrencyService {
        private final List<String> events;

        CombinedConcurrencyService(List<String> events) {
            this.events = events;
        }

        @Idempotent(key = "#p0")
        @Lock(key = "#p0")
        @RateLimit(key = "#p0")
        public void submit(String orderId) {
            events.add("business");
        }
    }
}
