package io.github.luminion.velo.idempotent.support;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式幂等处理器
 *
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RedissonIdempotentHandler implements IdempotentHandler {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryRecord(String key, String token, long timeout, TimeUnit unit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(token, Duration.ofNanos(unit.toNanos(timeout)));
    }

    @Override
    public void removeIfMatch(String key, String token) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        // compareAndSet 到 null 等价于"值匹配才删除"，由 Redisson 保证原子性
        bucket.compareAndSet(token, null);
    }

}
