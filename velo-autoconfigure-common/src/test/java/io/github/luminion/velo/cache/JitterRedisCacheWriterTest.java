package io.github.luminion.velo.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JitterRedisCacheWriterTest {

    @Test
    void shouldReturnSameInstanceWhenJitterDisabled() {
        RedisCacheWriter delegate = mock(RedisCacheWriter.class);
        assertThat(JitterRedisCacheWriter.wrap(delegate, 0)).isSameAs(delegate);
        assertThat(JitterRedisCacheWriter.wrap(delegate, -5)).isSameAs(delegate);
    }

    @Test
    void shouldKeepTtlWithinJitterRange() {
        Duration original = Duration.ofSeconds(100);
        for (int i = 0; i < 200; i++) {
            Duration jittered = JitterRedisCacheWriter.jitter(original, 20);
            assertThat(jittered.toMillis()).isBetween(80_000L, 120_000L);
        }
    }

    @Test
    void shouldNotJitterNullOrNonPositiveTtl() {
        assertThat(JitterRedisCacheWriter.jitter(null, 20)).isNull();
        assertThat(JitterRedisCacheWriter.jitter(Duration.ZERO, 20)).isEqualTo(Duration.ZERO);
        Duration negative = Duration.ofSeconds(-1);
        assertThat(JitterRedisCacheWriter.jitter(negative, 20)).isEqualTo(negative);
    }

    @Test
    void shouldApplyJitterToTtlOnPut() {
        RedisCacheWriter delegate = mock(RedisCacheWriter.class);
        AtomicReference<Duration> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(3));
            return null;
        }).when(delegate).put(any(), any(), any(), any());

        RedisCacheWriter writer = JitterRedisCacheWriter.wrap(delegate, 20);
        writer.put("cache", new byte[]{1}, new byte[]{2}, Duration.ofSeconds(100));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().toMillis()).isBetween(80_000L, 120_000L);
    }

    @Test
    void shouldDelegateNonWriteMethodsWithoutModification() {
        RedisCacheWriter delegate = mock(RedisCacheWriter.class);
        RedisCacheWriter writer = JitterRedisCacheWriter.wrap(delegate, 20);

        byte[] key = new byte[]{1};
        writer.get("cache", key);

        verify(delegate).get(eq("cache"), eq(key));
    }
}
