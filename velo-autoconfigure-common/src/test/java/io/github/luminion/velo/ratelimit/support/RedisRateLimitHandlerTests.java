package io.github.luminion.velo.ratelimit.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RedisRateLimitHandlerTests {

    @Test
    void shouldPassPerMillisecondFillRateToLuaScript() {
        CapturingRedisTemplate redisTemplate = new CapturingRedisTemplate();
        RedisRateLimitHandler handler = new RedisRateLimitHandler(redisTemplate);

        boolean acquired = handler.tryAcquire("demo", 2D, 1L, TimeUnit.SECONDS);

        assertThat(acquired).isTrue();
        assertThat(redisTemplate.keys).containsExactly("demo");
        assertThat(redisTemplate.args[0]).isEqualTo(2L);
        assertThat((Double) redisTemplate.args[1]).isCloseTo(0.002D, within(0.000_000_1D));
    }

    private static class CapturingRedisTemplate extends RedisTemplate<Object, Object> {
        private List<Object> keys;
        private Object[] args;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(RedisScript<T> script, List<Object> keys, Object... args) {
            this.keys = keys;
            this.args = args;
            return (T) Long.valueOf(1L);
        }
    }
}
