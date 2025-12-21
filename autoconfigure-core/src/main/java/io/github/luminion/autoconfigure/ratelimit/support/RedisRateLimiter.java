package io.github.luminion.autoconfigure.ratelimit.support;

import io.github.luminion.autoconfigure.ratelimit.annotation.RateLimit;
import io.github.luminion.autoconfigure.ratelimit.spi.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

/**
 * 基于Redis的令牌桶算法限流处理器。
 * <p>这是推荐的分布式限流实现，它平滑、精确，适合多节点环境。
 * <p><b>依赖:</b> {@code org.springframework.boot:spring-boot-starter-data-redis}
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rate_limit:";
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisScript<Long> limitScript = limitScript();

    private static DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    // 使用令牌桶算法Lua脚本，更平滑、更精确
    private static String limitScriptText() {
        //return "local key = KEYS[1]\n" +
        //        "local count = tonumber(ARGV[1])\n" +
        //        "local time = tonumber(ARGV[2])\n" +
        //        "local current = redis.call('get', key);\n" +
        //        "if current and tonumber(current) > count then\n" +
        //        "    return tonumber(current);\n" +
        //        "end\n" +
        //        "current = redis.call('incr', key)\n" +
        //        "if tonumber(current) == 1 then\n" +
        //        "    redis.call('expire', key, time)\n" +
        //        "end\n" +
        //        "return tonumber(current);";
        return "local key = KEYS[1]\n" +
                "local capacity = tonumber(ARGV[1])\n" +      // 令牌桶容量
                "local rate = tonumber(ARGV[2])\n" +          // 每秒生成的令牌数
                "local now_sec = tonumber(ARGV[3])\n" +       // 当前时间(秒)
                "local requested = 1\n" +                     // 本次请求消耗的令牌数
                "\n" +
                "local bucket_info = redis.call('hmget', key, 'tokens', 'timestamp')\n" +
                "local last_tokens = tonumber(bucket_info[1])\n" +
                "local last_timestamp = tonumber(bucket_info[2])\n" +
                "\n" +
                "if last_tokens == nil then\n" +
                "    last_tokens = capacity\n" +
                "    last_timestamp = now_sec\n" +
                "end\n" +
                "\n" +
                "local delta = math.max(0, now_sec - last_timestamp)\n" +
                "local new_tokens = math.min(capacity, last_tokens + delta * rate)\n" +
                "\n" +
                "if new_tokens >= requested then\n" +
                "    redis.call('hmset', key, 'tokens', new_tokens - requested, 'timestamp', now_sec)\n" +
                "    redis.call('expire', key, math.ceil(capacity / rate) * 2 + 1)\n" + // 设置一个合理的过期时间
                "    return 1\n" + // 允许
                "else\n" +
                "    redis.call('hmset', key, 'tokens', new_tokens, 'timestamp', now_sec)\n" +
                "    redis.call('expire', key, math.ceil(capacity / rate) * 2 + 1)\n" +
                "    return 0\n" + // 拒绝
                "end";
    }

    @Override
    public boolean tryAccess(String signature, RateLimit rateLimit) {
        String redisKey = KEY_PREFIX + signature;
        List<Object> keys = Collections.singletonList(redisKey);

        // 计算令牌桶参数
        double capacity = rateLimit.count();
        double rate = capacity / rateLimit.seconds();
        long nowInSeconds = System.currentTimeMillis() / 1000;

        Long result = redisTemplate.execute(limitScript, keys, capacity, rate, nowInSeconds);

        return result != null && result == 1L;
    }
}
