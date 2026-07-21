package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InvocationLogSupportTest {

    @Test
    void shouldKeepJsonNullAsResultPayload() {
        VeloProperties.InvocationProperties properties = new VeloProperties().getLog().getInvocation();

        assertThat(InvocationLogSupport.buildResultText(null, value -> "null", properties)).isEqualTo("null");

        properties.setIncludeResult(false);
        assertThat(InvocationLogSupport.buildResultText(null, value -> "null", properties))
                .isEqualTo(InvocationLogSupport.DISABLED_PAYLOAD);

        properties.setIncludeResult(true);
        properties.setMaxPayloadLength(0);
        assertThat(InvocationLogSupport.buildResultText(null, value -> "null", properties))
                .isEqualTo(InvocationLogSupport.DISABLED_PAYLOAD);
    }

    @Test
    void shouldSkipSerializationWhenPayloadLimitIsZero() {
        VeloProperties.InvocationProperties properties = new VeloProperties().getLog().getInvocation();
        properties.setMaxPayloadLength(0);
        RuntimeJsonSerializer serializer = value -> {
            throw new AssertionError("serializer should not be called");
        };

        assertThat(InvocationLogSupport.buildArgsText(null, null, new Object[0], serializer, properties))
                .isEqualTo(InvocationLogSupport.DISABLED_PAYLOAD);
        assertThat(InvocationLogSupport.buildResultText(new Object(), serializer, properties))
                .isEqualTo(InvocationLogSupport.DISABLED_PAYLOAD);
    }

    @Test
    void shouldUseReasonWhenPayloadSerializationFails() {
        VeloProperties.InvocationProperties properties = new VeloProperties().getLog().getInvocation();
        RuntimeJsonSerializer serializer = value -> {
            throw new IllegalStateException("boom");
        };

        assertThat(InvocationLogSupport.safeBuildResultText(new Object(), serializer, properties))
                .isEqualTo(InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD);
        assertThat(InvocationLogSupport.normalizePayload(null))
                .isEqualTo(InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD);
    }

    @Test
    void shouldCompareMillisecondSlowThresholdUsingMonotonicNanoseconds() {
        assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(
                TimeUnit.MICROSECONDS.toNanos(1500L), 1L)).isTrue();
        assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(
                TimeUnit.MICROSECONDS.toNanos(900L), 1L)).isFalse();
    }
}
