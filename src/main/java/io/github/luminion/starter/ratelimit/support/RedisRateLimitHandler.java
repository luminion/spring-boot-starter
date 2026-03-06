package io.github.luminion.starter.ratelimit.support;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式令牌桶限流器
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class RedisRateLimitHandler implements RateLimitHandler {

    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 令牌桶 Lua 脚本
     * KEYS[1]  : 限流 key
     * ARGV[1]  : capacity     令牌桶容量（窗口内最大请求数）
     * ARGV[2]  : fill_rate    每毫秒补充的令牌数 = capacity / (intervalMillis / 1000)
     * ARGV[3]  : now_ms       当前时间戳（毫秒）
     * ARGV[4]  : expire_sec   key 过期时间（秒）
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = createLimitScript();

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        long capacity = (long) Math.max(1, rate);
        long intervalMillis = unit.toMillis(timeout);
        // fillRate = rate / (timeout单位下的秒数)，即每秒补充的令牌数
        // 例如: 5次/分钟 -> fillRate = 5/60 ≈ 0.083 tokens/sec
        double fillRate = rate / (intervalMillis / 1000.0);
        long nowMs = System.currentTimeMillis();
        // 过期时间至少保留一个完整窗口 + 10s 冗余
        long expireSec = unit.toSeconds(timeout) + 10;

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
        // 使用毫秒时间戳，避免秒级精度不足的问题
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
                "-- 首次初始化\n" +
                "if tokens == nil then\n" +
                "    tokens  = capacity\n" +
                "    last_ms = now_ms\n" +
                "end\n" +
                "\n" +
                "-- 根据距上次请求的时间差补充令牌\n" +
                "local delta_ms      = math.max(0, now_ms - last_ms)\n" +
                "local granted_tokens = math.min(capacity, tokens + delta_ms * fill_rate)\n" +
                "\n" +
                "if granted_tokens >= 1 then\n" +
                "    redis.call('hmset', key, 'tokens', granted_tokens - 1, 'last_ms', now_ms)\n" +
                "    redis.call('expire', key, expire_sec)\n" +
                "    return 1\n" +
                "else\n" +
                "    -- 拒绝时不更新 last_ms，防止高频请求导致令牌永远无法恢复\n" +
                "    return 0\n" +
                "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
