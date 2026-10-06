package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis SET NX 的固定 TTL 锁，不续期。
 *
 * <p>同线程重入时校验 Redis 中的 owner token，不重置 TTL。业务必须在固定 TTL 内结束；
 * 到期不会中断业务，也不保证到期后的互斥。最外层解锁时按 token 原子删除，避免误删他人锁。</p>
 */
@Slf4j
public class RedisLockHandler implements LockHandler {

    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);
    private static final long DEFAULT_TTL_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    // 只保存当前线程正在持有的 token 和重入次数，不缓存空闲 key。
    private final ThreadLocal<Map<String, Hold>> holds = new ThreadLocal<>();

    public RedisLockHandler(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_TTL_SECONDS);
    }

    public RedisLockHandler(StringRedisTemplate redisTemplate, long ttlSeconds) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("Redis lock template must not be null.");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("Redis lock TTL seconds must be greater than zero.");
        }
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean tryLock(String key) {
        Map<String, Hold> current = holds.get();
        if (current == null) {
            // ThreadLocal.get() 也会建立空条目，未持锁时立即清理，异常或获取失败时不会残留。
            holds.remove();
        }
        Hold existing = current == null ? null : current.get(key);
        if (existing != null) {
            if (!existing.token.equals(redisTemplate.opsForValue().get(key))) {
                return false;
            }
            existing.depth++;
            return true;
        }

        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttlSeconds, TimeUnit.SECONDS);
        if (acquired == null) {
            throw new IllegalStateException("Redis lock acquisition was deferred by a transaction or pipeline.");
        }
        if (!acquired) {
            return false;
        }
        if (current == null) {
            current = new HashMap<>();
            holds.set(current);
        }
        current.put(key, new Hold(token));
        return true;
    }

    @Override
    public void unlock(String key) {
        Map<String, Hold> current = holds.get();
        if (current == null) {
            holds.remove();
            return;
        }
        Hold hold = current.get(key);
        if (hold == null || --hold.depth > 0) {
            return;
        }
        current.remove(key);
        try {
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), hold.token);
        } catch (Exception ex) {
            log.warn("Redis unlock failed for key: {}", key, ex);
        } finally {
            if (current.isEmpty()) {
                holds.remove();
            }
        }
    }

    private static final class Hold {
        private final String token;
        private int depth = 1;

        private Hold(String token) {
            this.token = token;
        }
    }
}
