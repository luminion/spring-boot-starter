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
            // 不先调用 isHeldByCurrentThread：该查询同样访问 Redis，查询失败会跳过 unlock，看门狗将持续续期。
            // 直接 unlock 时，Redisson 无论成功或失败都会停止当前线程的看门狗续期，锁最迟在看门狗超时后过期。
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            // 锁已过期或不属于当前线程，无需释放。
            log.debug("Redisson lock already released or not held by current thread for key: {}", key);
        } catch (Exception e) {
            log.warn("Redisson unlock failed for key: {}", key, e);
        }
    }

}
