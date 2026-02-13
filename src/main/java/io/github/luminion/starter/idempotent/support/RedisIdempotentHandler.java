package io.github.luminion.starter.idempotent.support;

import io.github.luminion.starter.core.spi.IdempotentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式幂等处理器
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class RedisIdempotentHandler implements IdempotentHandler {

    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        // 使用 SET NX EX 命令
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", timeout, unit);
        return success != null && success;
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(key);
    }
}
