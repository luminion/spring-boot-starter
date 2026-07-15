package io.github.luminion.velo.log;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InvocationLogSupportTest {

    @Test
    void shouldCompareMillisecondSlowThresholdUsingMonotonicNanoseconds() {
        assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(
                TimeUnit.MICROSECONDS.toNanos(1500L), 1L)).isTrue();
        assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(
                TimeUnit.MICROSECONDS.toNanos(900L), 1L)).isFalse();
    }
}
