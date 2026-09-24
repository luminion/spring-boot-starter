package io.github.luminion.velo.redis;

import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisBackendFailureTests {

    @Test
    @SuppressWarnings("unchecked")
    void redisOutageDoesNotSilentlySwitchToLocalBackend() {
        RedisConnectionFailureException outage = new RedisConnectionFailureException("Redis unavailable");
        StringRedisTemplate lockTemplate = mock(StringRedisTemplate.class);
        when(lockTemplate.opsForValue()).thenThrow(outage);
        try (RedisLockHandler lockHandler = new RedisLockHandler(lockTemplate)) {
            assertThatThrownBy(() -> lockHandler.lock("lock:key", 0, 1_000))
                    .isSameAs(outage);
        }

        RedisTemplate<Object, Object> idempotentTemplate = mock(RedisTemplate.class);
        when(idempotentTemplate.opsForValue()).thenThrow(outage);
        RedisIdempotentHandler idempotentHandler = new RedisIdempotentHandler(idempotentTemplate);
        assertThatThrownBy(() -> idempotentHandler.tryRecord("idempotent:key", "token", 1_000))
                .isSameAs(outage);

        StringRedisTemplate rateLimitTemplate = new StringRedisTemplate() {
            @Override
            public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
                throw outage;
            }
        };
        RedisRateLimitHandler rateLimitHandler = new RedisRateLimitHandler(rateLimitTemplate);
        assertThatThrownBy(() -> rateLimitHandler.tryAcquire("rate-limit:key", 1.0, 1_000))
                .isSameAs(outage);
    }
}
