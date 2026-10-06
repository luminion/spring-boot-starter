package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

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
    public boolean tryLock(String key) {
        // 不等待、不指定固定租期，生命周期和续期全部交给 Redisson 原生看门狗。
        return redissonClient.getLock(key).tryLock();
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
