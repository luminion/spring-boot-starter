package io.github.luminion.velo.lock.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.velo.lock.LockHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 Caffeine 的本地锁实现。
 *
 * 该实现只保证单 JVM 内互斥执行，不提供分布式一致性。
 */
@Slf4j
public class CaffeineLockHandler implements LockHandler {

    private final Cache<String, LockState> lockCache = Caffeine.newBuilder().build();

    public CaffeineLockHandler() {
        log.warn("[Velo Starter] CaffeineLockHandler is used as a local fallback implementation. " +
                "This handler only guarantees mutual exclusion inside a single JVM. " +
                "Use Redis or Redisson when cross-node locking is required.");
    }

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        LockState state = lockCache.asMap().compute(key, (k, existing) -> {
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
        LockState state = lockCache.getIfPresent(key);
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
        lockCache.asMap().computeIfPresent(key, (k, existing) -> {
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
