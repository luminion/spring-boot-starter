package io.github.luminion.velo.idempotent.support;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JdkIdempotentHandler implements IdempotentHandler {

    public JdkIdempotentHandler() {
        log.warn("[Velo Starter] JdkIdempotentHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause idempotent validation to fail. " +
                "Consider using Redis or Redisson for distributed idempotent validation.");
    }

    private final ConcurrentHashMap<String, Long> lockMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isCleaning = new AtomicBoolean(false);

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long expireAt = now + unit.toMillis(timeout);
        AtomicBoolean success = new AtomicBoolean(false);

        // compute 把“判断是否过期”和“写入新的过期时间”合并成一个原子操作，避免并发穿透。
        lockMap.compute(key, (k, v) -> {
            if (v == null || v <= now) {
                success.set(true);
                return expireAt;
            }
            return v;
        });

        // 过期清理只在 Map 膨胀后异步触发，避免请求主路径反复扫描整个表。
        if (lockMap.mappingCount() > 1024 && isCleaning.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    long currentTime = System.currentTimeMillis();
                    lockMap.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
                } finally {
                    isCleaning.set(false);
                }
            });
        }
        return success.get();
    }

    @Override
    public void unlock(String key) {
        lockMap.remove(key);
    }
}
