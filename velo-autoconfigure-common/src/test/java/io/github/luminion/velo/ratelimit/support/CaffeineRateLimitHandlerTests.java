package io.github.luminion.velo.ratelimit.support;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineRateLimitHandlerTests {

    @Test
    void shouldNotReinitializeBucketWhenTickerStartsAtZero() {
        MutableTicker ticker = new MutableTicker(0L);
        CaffeineRateLimitHandler handler = new CaffeineRateLimitHandler(ticker);

        assertThat(handler.tryAcquire("zero", 1D, 1_000L)).isTrue();
        assertThat(handler.tryAcquire("zero", 1D, 1_000L)).isFalse();
    }

    @Test
    void shouldKeepBucketForEntireLongWindow() {
        MutableTicker ticker = new MutableTicker();
        CaffeineRateLimitHandler handler = new CaffeineRateLimitHandler(ticker);

        assertThat(handler.tryAcquire("hourly", 1D, 3_600_000L)).isTrue();

        ticker.advance(6L, TimeUnit.MINUTES);
        assertThat(handler.tryAcquire("hourly", 1D, 3_600_000L)).isFalse();

        ticker.advance(54L, TimeUnit.MINUTES);
        assertThat(handler.tryAcquire("hourly", 1D, 3_600_000L)).isTrue();
    }

    private static final class MutableTicker implements Ticker {
        private final AtomicLong nanos;

        private MutableTicker() {
            this(1L);
        }

        private MutableTicker(long initialNanos) {
            this.nanos = new AtomicLong(initialNanos);
        }

        @Override
        public long read() {
            return nanos.get();
        }

        private void advance(long time, TimeUnit unit) {
            nanos.addAndGet(unit.toNanos(time));
        }
    }
}
