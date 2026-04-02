package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class JdkLockHandler implements LockHandler {

    public JdkLockHandler() {
        log.warn("[Velo Starter] JdkLockHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause lock validation to fail. " +
                "Consider using Redis or Redisson for distributed locking.");
    }

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
