package io.github.luminion.starter.idempotent.support;

import io.github.luminion.starter.idempotent.IdempotentHandler;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式幂等处理器
 *
 * @author luminion
 * @since 1.0.1
 */
@RequiredArgsConstructor
public class RedissonIdempotentHandler implements IdempotentHandler {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent("LOCKED", java.time.Duration.of(timeout, unit.toChronoUnit()));
    }

    @Override
    public void release(String key) {
        redissonClient.getBucket(key).delete();
    }
}
