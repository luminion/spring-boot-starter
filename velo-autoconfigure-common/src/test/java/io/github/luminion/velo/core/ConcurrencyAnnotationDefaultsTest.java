package io.github.luminion.velo;

import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyAnnotationDefaultsTest {

    @Test
    void shouldExposeSafeIdempotentDefaults() throws NoSuchMethodException {
        Idempotent idempotent = annotation("idempotent", Idempotent.class);

        assertThat(idempotent.ttl()).isEqualTo(3);
    }

    @Test
    void shouldExposeSafeLockDefaults() throws NoSuchMethodException {
        Lock lock = annotation("lock", Lock.class);

        assertThat(lock.waitTimeout()).isZero();
        assertThat(lock.lease()).isEqualTo(30);
    }

    @Test
    void shouldExposeSafeRateLimitDefaults() throws NoSuchMethodException {
        RateLimit rateLimit = annotation("rateLimit", RateLimit.class);

        assertThat(rateLimit.permits()).isEqualTo(50);
        assertThat(rateLimit.ttl()).isEqualTo(1);
    }

    private static <A extends java.lang.annotation.Annotation> A annotation(String methodName, Class<A> type)
            throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod(methodName);
        return method.getAnnotation(type);
    }

    static class SampleService {

        @Idempotent
        void idempotent() {
        }

        @Lock
        void lock() {
        }

        @RateLimit
        void rateLimit() {
        }
    }
}
