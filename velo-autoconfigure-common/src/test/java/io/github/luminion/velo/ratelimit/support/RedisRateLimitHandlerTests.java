package io.github.luminion.velo.ratelimit.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RedisRateLimitHandlerTests {

    @Test
    void shouldPassPerMillisecondFillRateToLuaScript() {
        CapturingRedisTemplate redisTemplate = new CapturingRedisTemplate();
        RedisRateLimitHandler handler = new RedisRateLimitHandler(redisTemplate);

        boolean acquired = handler.tryAcquire("demo", 2D, 1000L);

        assertThat(acquired).isTrue();
        assertThat(redisTemplate.keys).containsExactly("demo");
        assertThat(redisTemplate.args[0]).isEqualTo("2");
        assertThat(Double.parseDouble((String) redisTemplate.args[1]))
                .isCloseTo(0.002D, within(0.000_000_1D));
        assertThat(redisTemplate.args).hasSize(3);
        assertThat(redisTemplate.scriptText).contains("redis.call('time')");
        assertThat(redisTemplate.scriptText).doesNotContain("ARGV[4]");
    }

    private static class CapturingRedisTemplate extends StringRedisTemplate {
        private List<String> keys;
        private Object[] args;
        private String scriptText;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            this.keys = keys;
            this.args = args;
            this.scriptText = script.getScriptAsString();
            return (T) Long.valueOf(1L);
        }
    }
}
