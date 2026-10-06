package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 ReentrantLock 的本地锁，仅在当前 JVM 内互斥。
 * 活跃锁按 key 共享，最后一个持有者或获取调用离开后立即移除；不需要缓存过期或后台清理。
 *
 * @author luminion
 * @since 1.0.0
 */
public class JdkLockHandler implements LockHandler {

    private final ConcurrentHashMap<String, LockState> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key) {
        LockState state = lockMap.compute(key, (k, existing) -> {
            LockState resolved = existing != null ? existing : new LockState();
            resolved.retain();
            return resolved;
        });

        boolean locked = false;
        try {
            locked = state.lock.tryLock();
            return locked;
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
        if (state.lock.isHeldByCurrentThread()) {
            try {
                state.lock.unlock();
            } finally {
                releaseState(key, state);
            }
        }
    }

    private void releaseState(String key, LockState state) {
        lockMap.computeIfPresent(key, (k, existing) -> {
            if (existing != state) {
                return existing;
            }
            return state.release() == 0 && !state.lock.isLocked()
                    ? null : state;
        });
    }

    private static final class LockState {
        private final ReentrantLock lock = new ReentrantLock();
        // 引用计数仅在同一 key 的 ConcurrentHashMap.compute 内修改。
        private int references;

        private void retain() {
            references++;
        }

        private int release() {
            return --references;
        }
    }
}
