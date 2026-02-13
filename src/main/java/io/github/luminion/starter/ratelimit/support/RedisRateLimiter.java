package io.github.luminion.starter.ratelimit.support;

import io.github.luminion.starter.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

/**
 * 基于 Redis 的分布式令牌桶限流器
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisScript<Long> limitScript = createLimitScript();

    @Override
    public boolean tryAcquire(String key, double rate, double burst) {
        // Lua 脚本需要的参数：[key], [capacity], [rate], [now_sec]
        long now = System.currentTimeMillis() / 1000;

        Long result = redisTemplate.execute(
                limitScript,
                Collections.singletonList(key),
                (long) burst,
                (long) rate,
                now);

        return result != null && result == 1L;
    }

    private static RedisScript<Long> createLimitScript() {
        String script = "local key = KEYS[1]\n" +
                "local capacity = tonumber(ARGV[1])\n" +
                "local rate = tonumber(ARGV[2])\n" +
                "local now_sec = tonumber(ARGV[3])\n" +
                "\n" +
                "local bucket = redis.call('hmget', key, 'tokens', 'last_time')\n" +
                "local tokens = tonumber(bucket[1])\n" +
                "local last_time = tonumber(bucket[2])\n" +
                "\n" +
                "if tokens == nil then\n" +
                "    tokens = capacity\n" +
                "    last_time = now_sec\n" +
                "end\n" +
                "\n" +
                "local delta = math.max(0, now_sec - last_time)\n" +
                "local granted_tokens = math.min(capacity, tokens + delta * rate)\n" +
                "\n" +
                "if granted_tokens >= 1 then\n" +
                "    redis.call('hmset', key, 'tokens', granted_tokens - 1, 'last_time', now_sec)\n" +
                "    redis.call('expire', key, math.ceil(capacity / rate) + 10)\n" +
                "    return 1\n" +
                "else\n" +
                "    redis.call('hmset', key, 'tokens', granted_tokens, 'last_time', now_sec)\n" +
                "    return 0\n" +
                "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
