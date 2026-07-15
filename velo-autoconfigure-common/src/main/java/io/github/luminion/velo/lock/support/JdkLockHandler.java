package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    private final ConcurrentHashMap<String, LockState> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean lock(String key, long waitTime, long leaseTime) {
        LockState state = lockMap.compute(key, (k, existing) -> {
            LockState resolved = existing != null ? existing : new LockState();
            resolved.retain();
            return resolved;
        });

        boolean locked = false;
        try {
            locked = state.lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
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
