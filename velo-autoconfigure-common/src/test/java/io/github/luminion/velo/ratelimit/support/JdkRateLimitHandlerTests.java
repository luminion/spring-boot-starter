package io.github.luminion.velo.ratelimit.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class JdkRateLimitHandlerTests {

    @Test
    void shouldNotReinitializeBucketWhenClockStartsAtZero() {
        AtomicLong nanoTime = new AtomicLong(0L);
        JdkRateLimitHandler handler = new JdkRateLimitHandler(nanoTime::get);

        try {
            assertThat(handler.tryAcquire("zero", 1D, 1_000L)).isTrue();
            assertThat(handler.tryAcquire("zero", 1D, 1_000L)).isFalse();
        } finally {
            handler.destroy();
        }
    }
}
