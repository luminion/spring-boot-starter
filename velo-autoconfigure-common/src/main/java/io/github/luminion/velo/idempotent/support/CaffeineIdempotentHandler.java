package io.github.luminion.velo.idempotent.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.luminion.velo.idempotent.IdempotentHandler;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地幂等处理器
 * 通过存储过期时间戳并利用 asMap().compute 方法保证原子性
 *
 * @author luminion
 */
public class CaffeineIdempotentHandler implements IdempotentHandler {

    // 仅按 TTL 过期，不设 maximumSize：容量驱逐会赶走仍在幂等窗口内的记录，
    // 导致被驱逐 key 的重复请求重新通过，破坏“TTL 内拒绝重复提交”语义。
    // 每个 key 的存活时间由自定义 Expiry 按其 TTL 独立控制，到期即自动清理。
    private final Cache<String, Marker> cache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Marker>() {
                @Override
                public long expireAfterCreate(String key, Marker marker, long currentTime) {
                    return marker.ttlNanos();
                }
                @Override
                public long expireAfterUpdate(String key, Marker marker, long currentTime, long currentDuration) {
                    return currentDuration;
                }
                @Override
                public long expireAfterRead(String key, Marker marker, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    @Override
    public boolean tryRecord(String key, String token, long timeout) {
        Marker existing = cache.asMap().putIfAbsent(
                key, new Marker(token, TimeUnit.MILLISECONDS.toNanos(timeout)));
        return existing == null;
    }

    @Override
    public void removeIfMatch(String key, String token) {
        // 仅当存储的 token 与传入一致时才删除，避免误删并发请求写入的新记录
        cache.asMap().computeIfPresent(key, (k, marker) ->
                marker.token().equals(token) ? null : marker);
    }

    private static final class Marker {
        private final String token;
        private final long ttlNanos;

        private Marker(String token, long ttlNanos) {
            this.token = token;
            this.ttlNanos = ttlNanos;
        }

        private String token() {
            return token;
        }

        private long ttlNanos() {
            return ttlNanos;
        }
    }
}
