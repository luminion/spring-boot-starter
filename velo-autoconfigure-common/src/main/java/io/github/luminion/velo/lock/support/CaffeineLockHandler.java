package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 Caffeine 场景下的本地锁实现（兜底方案）。
 * <p>
 * 该实现只保证单 JVM 内互斥执行，不提供分布式一致性。
 * <p>
 * 用 {@link ConcurrentHashMap} + 引用计数管理 LockState，而非带淘汰策略的 Caffeine 缓存：
 * Caffeine 的 {@code maximumSize} / {@code expireAfterAccess} 淘汰由缓存内部触发、感知不到引用计数，
 * 可能在锁仍被持有时淘汰 LockState，下个线程拿到全新 {@link ReentrantLock} 从而打破互斥。
 * 引用计数在 lock 时 retain、unlock 时 release，计数归零且未被持有才移除，生命周期与 lock/unlock 配对绑定，
 * 不依赖时间或容量淘汰，也不会泄漏。
 */
@Slf4j
public class CaffeineLockHandler implements LockHandler {

    private final ConcurrentHashMap<String, LockState> lockMap = new ConcurrentHashMap<>();

    public CaffeineLockHandler() {
        log.warn("[Velo Starter] CaffeineLockHandler is used as a local fallback implementation. " +
                "This handler only guarantees mutual exclusion inside a single JVM. " +
                "Use Redis or Redisson when cross-node locking is required.");
    }

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        LockState state = lockMap.compute(key, (k, existing) -> {
            LockState resolved = existing != null ? existing : new LockState();
            resolved.retain();
            return resolved;
        });

        boolean locked = false;
        try {
            locked = state.lock.tryLock(waitTime, unit);
            return locked;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (!locked) {
                releaseState(key, state);
            }
        }
    }

    @Override
    public void unlock(String key) {
        LockState state = lockMap.get(key);
        if (state == null) {
            return;
        }
        try {
            if (state.lock.isHeldByCurrentThread()) {
                state.lock.unlock();
            }
        } finally {
            releaseState(key, state);
        }
    }

    private void releaseState(String key, LockState state) {
        lockMap.computeIfPresent(key, (k, existing) -> {
            if (existing != state) {
                return existing;
            }
            return state.release() == 0 && !state.lock.isLocked() ? null : state;
        });
    }

    private static final class LockState {
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicInteger references = new AtomicInteger();

        private void retain() {
            references.incrementAndGet();
        }

        private int release() {
            return references.updateAndGet(current -> current > 0 ? current - 1 : 0);
        }
    }
}
