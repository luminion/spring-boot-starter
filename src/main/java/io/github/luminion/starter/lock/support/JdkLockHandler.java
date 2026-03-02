package io.github.luminion.starter.lock.support;

import io.github.luminion.starter.lock.LockHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 ReentrantLock 的本地锁实现 (兜底方案)
 *
 * @author luminion
 * @since 1.0.0
 */
public class JdkLockHandler implements LockHandler {

    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            return lock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        ReentrantLock lock = lockMap.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            // 注意：这里不移除 Map 中的 Key，因为可能有其他线程正在尝试获取锁
            // 在生产环境可以配合清理逻辑定期移除长期不用的 Key
        }
    }
}
