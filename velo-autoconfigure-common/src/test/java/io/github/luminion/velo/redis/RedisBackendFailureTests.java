package io.github.luminion.velo.redis;

import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class RedisBackendFailureTests {

    @Test
    @SuppressWarnings("unchecked")
    void redisOutageDoesNotSilentlySwitchToLocalBackend() {
        RedisConnectionFailureException outage = new RedisConnectionFailureException("Redis unavailable");
        StringRedisTemplate lockTemplate = mock(StringRedisTemplate.class);
        when(lockTemplate.opsForValue()).thenThrow(outage);
        RedisLockHandler lockHandler = new RedisLockHandler(lockTemplate);
        assertThatThrownBy(() -> lockHandler.tryLock("lock:key")).isSameAs(outage);

        RedisTemplate<Object, Object> idempotentTemplate = mock(RedisTemplate.class);
        when(idempotentTemplate.opsForValue()).thenThrow(outage);
        RedisIdempotentHandler idempotentHandler = new RedisIdempotentHandler(idempotentTemplate);
        assertThatThrownBy(() -> idempotentHandler.tryRecord("idempotent:key", "token", 1_000))
                .isSameAs(outage);

        StringRedisTemplate rateTemplate = mock(StringRedisTemplate.class);
        when(rateTemplate.execute(any(RedisScript.class), eq(Collections.singletonList("rate:key"))))
                .thenThrow(outage);
        assertThatThrownBy(() -> new RedisRateLimitHandler(rateTemplate).tryAcquire("rate:key", 1))
                .isSameAs(outage);

    }
}
