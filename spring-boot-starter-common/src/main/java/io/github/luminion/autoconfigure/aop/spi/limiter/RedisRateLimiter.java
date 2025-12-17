package io.github.luminion.autoconfigure.aop.spi.limiter;

import io.github.luminion.autoconfigure.aop.annotation.RateLimit;
import io.github.luminion.autoconfigure.aop.spi.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;


/**
 * RedisRateLimiter
 * 需要引入redis依赖
 * 实现参考自RuoYi
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisScript<Long> limitScript = limitScript();

    private static DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }
    private static String limitScriptText() {
        return "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local time = tonumber(ARGV[2])\n" +
                "local current = redis.call('get', key);\n" +
                "if current and tonumber(current) > count then\n" +
                "    return tonumber(current);\n" +
                "end\n" +
                "current = redis.call('incr', key)\n" +
                "if tonumber(current) == 1 then\n" +
                "    redis.call('expire', key, time)\n" +
                "end\n" +
                "return tonumber(current);";
    }
    
    @Override
    public boolean doLimit(String signature, RateLimit rateLimit) {
        List<Object> keys = Collections.singletonList(signature);
        int count = rateLimit.count();
        int seconds = rateLimit.seconds();
        Long number = redisTemplate.execute(limitScript, keys, count, seconds);
        return number.intValue() <= count;

    }
}
