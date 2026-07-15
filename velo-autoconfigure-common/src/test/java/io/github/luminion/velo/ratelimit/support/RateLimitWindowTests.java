package io.github.luminion.velo.ratelimit.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitWindowTests {

    @Test
    void shouldKeepIntegerPermitsOnOriginalWindow() {
        RateLimitWindow window = RateLimitWindow.from(2D, 1000L);

        assertThat(window.capacity()).isEqualTo(2L);
        assertThat(window.intervalMillis()).isEqualTo(1000L);
        assertThat(window.intervalNanos()).isEqualTo(1_000_000_000L);
    }

    @Test
    void shouldNormalizeFractionalPermitsToSharedBurstCapacity() {
        RateLimitWindow window = RateLimitWindow.from(1.5D, 1000L);

        assertThat(window.capacity()).isEqualTo(2L);
        assertThat(window.intervalMillis()).isEqualTo(1334L);
        assertThat(window.intervalNanos()).isEqualTo(1_333_333_334L);
    }
}
