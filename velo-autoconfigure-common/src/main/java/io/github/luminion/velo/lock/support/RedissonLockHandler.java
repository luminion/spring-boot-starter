package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockToken;
import io.github.luminion.velo.lock.ReactiveLockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Redisson 的分布式锁实现
 * 支持重入、看门狗续期等高级特性
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonLockHandler implements ReactiveLockHandler {

    private static final AtomicLong REACTIVE_THREAD_ID = new AtomicLong(1_000_000_000L);

    private final RedissonClient redissonClient;

    @Override
    public boolean lock(String key, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(key);
        try {
            // waitTime: 等待获取锁的最大时间
            // leaseTime: 释放锁的时间
            // 如果 leaseTime 为 -1，则会启用看门狗机制
            return lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
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

    @Override
    public CompletionStage<LockToken> lockToken(String key, long waitTime, long leaseTime) {
        long threadId = REACTIVE_THREAD_ID.getAndIncrement();
        RLock lock = redissonClient.getLock(key);
        // Redisson 的异步 API 允许显式传递 threadId，避免响应式信号线程变化导致无法释放锁。
        return lock.tryLockAsync(waitTime, leaseTime, TimeUnit.MILLISECONDS, threadId)
                .thenApply(acquired -> Boolean.TRUE.equals(acquired) ? new LockToken(key, threadId) : null)
                .exceptionally(error -> {
                    log.error("Redisson reactive lock error, key: {}", key, error);
                    return null;
                });
    }

    @Override
    public CompletionStage<Void> unlockToken(LockToken token) {
        if (token == null || !(token.getOwner() instanceof Long)) {
            return CompletableFuture.completedFuture(null);
        }
        long threadId = (Long) token.getOwner();
        RLock lock = redissonClient.getLock(token.getKey());
        return lock.isHeldByThreadAsync(threadId)
                .thenCompose(held -> Boolean.TRUE.equals(held)
                        ? lock.unlockAsync(threadId)
                        : CompletableFuture.completedFuture(null))
                .exceptionally(error -> {
                    log.warn("Redisson reactive unlock failed for key: {}", token.getKey(), error);
                    return null;
                });
    }
}
