package io.github.luminion.velo.idempotent.support;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JdkIdempotentHandler implements IdempotentHandler, DisposableBean {

    public JdkIdempotentHandler() {
        log.warn("[Velo Starter] JdkIdempotentHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause idempotent validation to fail. " +
                "Consider using Redis or Redisson for distributed idempotent validation.");
    }

    private final ConcurrentHashMap<String, Record> recordMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isCleaning = new AtomicBoolean(false);
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "velo-idempotent-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Override
    public boolean tryRecord(String key, String token, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long expireAt = now + unit.toMillis(timeout);
        AtomicBoolean success = new AtomicBoolean(false);

        // compute 把"判断是否过期"和"写入新的过期时间"合并成一个原子操作，避免并发穿透。
        recordMap.compute(key, (k, v) -> {
            if (v == null || v.expireAt <= now) {
                success.set(true);
                return new Record(token, expireAt);
            }
            return v;
        });

        // 过期清理只在 Map 膨胀后异步触发，避免请求主路径反复扫描整个表。
        if (recordMap.mappingCount() > 1024 && isCleaning.compareAndSet(false, true)) {
            try {
                cleanupExecutor.execute(() -> {
                    try {
                        long currentTime = System.currentTimeMillis();
                        recordMap.entrySet().removeIf(entry -> entry.getValue().expireAt <= currentTime);
                    } finally {
                        isCleaning.set(false);
                    }
                });
            } catch (RejectedExecutionException ignored) {
                // 容器关闭后线程池已 shutdown，清理提交被拒属正常；复位标志避免永久 true 再不触发清理
                isCleaning.set(false);
            }
        }
        return success.get();
    }

    @Override
    public void removeIfMatch(String key, String token) {
        // 仅当存储的 token 与传入一致时才删除，避免误删并发请求写入的新记录
        recordMap.computeIfPresent(key, (k, v) -> v.token.equals(token) ? null : v);
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdownNow();
    }

    private static final class Record {
        private final String token;
        private final long expireAt;

        private Record(String token, long expireAt) {
            this.token = token;
            this.expireAt = expireAt;
        }
    }
}
