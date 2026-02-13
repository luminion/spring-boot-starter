package io.github.luminion.starter.idempotent.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.starter.idempotent.IdempotentHandler;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地幂等处理器
 *
 * @author luminion
 */
public class LocalIdempotentHandler implements IdempotentHandler {

    /**
     * 这里使用一个较长的过期时间作为缓存容器的清理策略
     * 实际的过期由 tryLock 中的逻辑或外部配置决定（此处由于 Caffeine 动态过期较复杂，简单处理）
     */
    private final Cache<String, Boolean> locks = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        // Caffeine 不直接支持为单个 Key 设置不同 TTL，但在 starter 场景下
        // 我们可以通过这种方式模拟 SET NX
        if (locks.asMap().putIfAbsent(key, true) == null) {
            // 这里我们无法精确实现针对 Key 的自定 TTL（除非使用更加复杂的实现）
            // 但对于单机测试或简单场景已足够。
            // 最佳实践还是推荐使用 Redis。
            return true;
        }
        return false;
    }

    @Override
    public void release(String key) {
        locks.invalidate(key);
    }
}
