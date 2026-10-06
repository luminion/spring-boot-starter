package io.github.luminion.velo.ratelimit.support;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuavaRateLimitHandlerTests {

    @Test
    void repeatedRequestsShareQuotaWhileDifferentKeysAreIndependent() {
        GuavaRateLimitHandler handler = new GuavaRateLimitHandler();
        assertThat(handler.tryAcquire("first", 1)).isTrue();
        assertThat(handler.tryAcquire("first", 1)).isFalse();
        assertThat(handler.tryAcquire("second", 1)).isTrue();
    }

    @Test
    void idleKeysExpireUsingGuavaNativeCache() {
        MutableTicker ticker = new MutableTicker();
        GuavaRateLimitHandler handler = new GuavaRateLimitHandler(ticker);
        assertThat(handler.tryAcquire("key", 1)).isTrue();
        assertThat(handler.tryAcquire("key", 1)).isFalse();
        ticker.advance(5);
        assertThat(handler.tryAcquire("key", 1)).isTrue();
    }

    @Test
    void accessesKeepActiveKeysFromExpiring() {
        MutableTicker ticker = new MutableTicker();
        GuavaRateLimitHandler handler = new GuavaRateLimitHandler(ticker);
        assertThat(handler.tryAcquire("key", 1)).isTrue();
        ticker.advance(4);
        assertThat(handler.tryAcquire("key", 1)).isFalse();
        ticker.advance(4);
        assertThat(handler.tryAcquire("key", 1)).isFalse();
    }

    @Test
    void changingRateUsesNativeSetRateWithoutDiscardingPreviousDebt() {
        GuavaRateLimitHandler handler = new GuavaRateLimitHandler();
        assertThat(handler.tryAcquire("key", 1)).isTrue();
        assertThat(handler.tryAcquire("key", 2)).isFalse();
    }

    @Test
    void nonPositiveQpsIsRejected() {
        GuavaRateLimitHandler handler = new GuavaRateLimitHandler();
        assertThatThrownBy(() -> handler.tryAcquire("key", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> handler.tryAcquire("key", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableTicker extends Ticker {
        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        private void advance(long minutes) {
            nanos += TimeUnit.MINUTES.toNanos(minutes);
        }
    }
}
