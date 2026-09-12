package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockToken;
import io.github.luminion.velo.lock.ReactiveLockHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.UUID;

/**
 * 基于 ReentrantLock 的本地锁实现 (兜底方案)
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
public class JdkLockHandler implements ReactiveLockHandler {

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

        long startNanos = System.nanoTime();
        long waitNanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
        boolean gateAcquired = false;
        boolean locked = false;
        try {
            // 首次进入先占用统一闸门，使响应式令牌锁与同步 ReentrantLock 互斥；同线程重入不重复获取闸门。
            if (!state.lock.isHeldByCurrentThread()) {
                gateAcquired = state.reactiveGate.tryAcquire(waitNanos, TimeUnit.NANOSECONDS);
                if (!gateAcquired) {
                    return false;
                }
            }
            long remainingNanos = remainingNanos(startNanos, waitNanos);
            locked = state.lock.tryLock(Math.max(0L, remainingNanos), TimeUnit.NANOSECONDS);
            return locked;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (!locked && gateAcquired) {
                state.reactiveGate.release();
            }
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
                boolean outermost = state.lock.getHoldCount() == 1;
                state.lock.unlock();
                if (outermost) {
                    state.reactiveGate.release();
                }
            }
        } finally {
            releaseState(key, state);
        }
    }

    @Override
    public CompletionStage<LockToken> lockToken(String key, long waitTime, long leaseTime) {
        LockState state = lockMap.compute(key, (k, existing) -> {
            LockState resolved = existing != null ? existing : new LockState();
            resolved.retain();
            return resolved;
        });

        long waitNanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
        boolean locked = false;
        String owner = UUID.randomUUID().toString();
        try {
            locked = state.reactiveGate.tryAcquire(waitNanos, TimeUnit.NANOSECONDS);
            if (!locked) {
                return CompletableFuture.completedFuture(null);
            }
            state.reactiveOwner = owner;
            return CompletableFuture.completedFuture(new LockToken(key, owner));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(null);
        } finally {
            if (!locked) {
                releaseState(key, state);
            }
        }
    }

    @Override
    public CompletionStage<Void> unlockToken(LockToken token) {
        if (token == null || !(token.getOwner() instanceof String)) {
            return CompletableFuture.completedFuture(null);
        }
        String key = token.getKey();
        LockState state = lockMap.get(key);
        if (state == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            synchronized (state) {
                if (!((String) token.getOwner()).equals(state.reactiveOwner)) {
                    return CompletableFuture.completedFuture(null);
                }
                state.reactiveOwner = null;
                state.reactiveGate.release();
            }
        } finally {
            releaseState(key, state);
        }
        return CompletableFuture.completedFuture(null);
    }

    private static long remainingNanos(long startNanos, long waitNanos) {
        if (waitNanos <= 0L) {
            return 0L;
        }
        long elapsedNanos = System.nanoTime() - startNanos;
        if (elapsedNanos >= waitNanos) {
            return 0L;
        }
        return waitNanos - elapsedNanos;
    }

    private void releaseState(String key, LockState state) {
        lockMap.computeIfPresent(key, (k, existing) -> {
            if (existing != state) {
                return existing;
            }
            return state.release() == 0 && !state.lock.isLocked()
                    && state.reactiveGate.availablePermits() == 1 && state.reactiveOwner == null ? null : state;
        });
    }

    private static final class LockState {
        private final ReentrantLock lock = new ReentrantLock();
        private final Semaphore reactiveGate = new Semaphore(1, true);
        private final AtomicInteger references = new AtomicInteger();
        private volatile String reactiveOwner;

        private void retain() {
            references.incrementAndGet();
        }

        private int release() {
            return references.updateAndGet(current -> current > 0 ? current - 1 : 0);
        }
    }
}
