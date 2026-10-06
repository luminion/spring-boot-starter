package io.github.luminion.velo.ratelimit.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisRateLimitHandlerTests {

    @Test
    @SuppressWarnings("unchecked")
    void scriptCountsAreComparedToQps() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList("key"))))
                .thenReturn(1L, 2L, 3L);
        RedisRateLimitHandler handler = new RedisRateLimitHandler(template);
        assertThat(handler.tryAcquire("key", 2)).isTrue();
        assertThat(handler.tryAcquire("key", 2)).isTrue();
        assertThat(handler.tryAcquire("key", 2)).isFalse();
    }

    @Test
    void deferredScriptResultIsRejected() {
        RedisRateLimitHandler handler = new RedisRateLimitHandler(mock(StringRedisTemplate.class));
        assertThatThrownBy(() -> handler.tryAcquire("key", 1)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transactions and pipelines");
    }

    @Test
    void invalidQpsDoesNotAccessRedis() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RedisRateLimitHandler handler = new RedisRateLimitHandler(template);
        assertThatThrownBy(() -> handler.tryAcquire("key", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> handler.tryAcquire("key", -1)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(template);
    }
}
