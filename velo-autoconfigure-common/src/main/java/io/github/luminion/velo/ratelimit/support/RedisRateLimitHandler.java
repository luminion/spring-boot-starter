package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisRateLimitHandler implements RateLimitHandler {

    private final RedisTemplate<Object, Object> redisTemplate;
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = createLimitScript();

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        long capacity = window.capacity();
        long intervalMillis = window.intervalMillis();

        // 脚本内部按毫秒时间差补充令牌，这里提前把速率换算到“每毫秒恢复多少令牌”。
        double fillRate = capacity / (double) intervalMillis;
        long nowMs = System.currentTimeMillis();

        // 令牌桶至少保留一个完整窗口，再加一点冗余，避免边界时间频繁初始化。
        long expireSec = Math.max(1L, (intervalMillis + 999L) / 1000L) + 10L;

        Long result = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(key),
                capacity,
                fillRate,
                nowMs,
                expireSec
        );

        return Long.valueOf(1L).equals(result);
    }

    private static RedisScript<Long> createLimitScript() {
        String script =
                "local key        = KEYS[1]\n" +
                "local capacity   = tonumber(ARGV[1])\n" +
                "local fill_rate  = tonumber(ARGV[2])\n" +
                "local now_ms     = tonumber(ARGV[3])\n" +
                "local expire_sec = tonumber(ARGV[4])\n" +
                "\n" +
                "local bucket    = redis.call('hmget', key, 'tokens', 'last_ms')\n" +
                "local tokens    = tonumber(bucket[1])\n" +
                "local last_ms   = tonumber(bucket[2])\n" +
                "\n" +
                "-- 第一次访问时直接补满令牌桶\n" +
                "if tokens == nil then\n" +
                "    tokens  = capacity\n" +
                "    last_ms = now_ms\n" +
                "end\n" +
                "\n" +
                "-- 按时间差恢复令牌，但不会超过容量上限\n" +
                "local delta_ms      = math.max(0, now_ms - last_ms)\n" +
                "local granted_tokens = math.min(capacity, tokens + delta_ms * fill_rate)\n" +
                "\n" +
                "if granted_tokens >= 1 then\n" +
                "    redis.call('hmset', key, 'tokens', granted_tokens - 1, 'last_ms', now_ms)\n" +
                "    redis.call('expire', key, expire_sec)\n" +
                "    return 1\n" +
                "else\n" +
                "    -- 拒绝时不推进 last_ms，避免持续空打把恢复时间不断往后拖\n" +
                "    return 0\n" +
                "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
