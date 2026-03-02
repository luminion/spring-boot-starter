package io.github.luminion.starter.lock.support;

import io.github.luminion.starter.lock.LockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式锁实现
 * 支持重入、看门狗续期等高级特性
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonLockHandler implements LockHandler {

    private final RedissonClient redissonClient;

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            // waitTime: 等待获取锁的最大时间
            // leaseTime: 释放锁的时间
            // 如果 leaseTime 为 -1，则会启用看门狗机制
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("Redisson lock error, key: {}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("Redisson unlock failed or lock already released for key: {}", key, e);
        }
    }
}
