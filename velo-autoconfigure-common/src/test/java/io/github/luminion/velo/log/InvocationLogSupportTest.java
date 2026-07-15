package io.github.luminion.velo.log;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InvocationLogSupportTest {

    @Test
    void shouldCompareSubMillisecondSlowThresholdAtNanosecondPrecision() {
        assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(
                TimeUnit.MICROSECONDS.toNanos(900L), 500L, TimeUnit.MICROSECONDS)).isTrue();
        assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(
                TimeUnit.MICROSECONDS.toNanos(400L), 500L, TimeUnit.MICROSECONDS)).isFalse();
    }
}
