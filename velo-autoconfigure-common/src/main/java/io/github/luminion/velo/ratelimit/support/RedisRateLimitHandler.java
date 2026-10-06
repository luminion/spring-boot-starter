package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * Redis 固定一秒窗口限流，所有实例共享计数，首次请求开始窗口。
 * <p>计数和首次设置过期时间通过 Lua 原子执行，过期由 Redis 管理。
 * 允许窗口边界突发：相邻窗口的额度可在短时间内连续使用，不能保证任意滚动一秒均不超过 QPS。
 */
public class RedisRateLimitHandler implements RateLimitHandler {

    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('incr', KEYS[1]); "
                    + "if current == 1 then redis.call('expire', KEYS[1], 1) end; return current;", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitHandler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int qps) {
        if (qps <= 0) {
            throw new IllegalArgumentException("qps must be positive");
        }
        Long count = redisTemplate.execute(ACQUIRE_SCRIPT, Collections.singletonList(key));
        if (count == null) {
            throw new IllegalStateException("Redis rate limiting requires an immediate script result; "
                    + "transactions and pipelines are not supported");
        }
        return count <= qps;
    }
}
