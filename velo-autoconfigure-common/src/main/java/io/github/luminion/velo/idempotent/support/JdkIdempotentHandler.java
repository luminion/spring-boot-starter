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
import java.util.function.LongSupplier;

@Slf4j
public class JdkIdempotentHandler implements IdempotentHandler, DisposableBean {

    public JdkIdempotentHandler() {
        this(System::nanoTime);
    }

    JdkIdempotentHandler(LongSupplier nanoTimeSupplier) {
        this.nanoTimeSupplier = nanoTimeSupplier;
        log.warn("[Velo Starter] JdkIdempotentHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause idempotent validation to fail. " +
                "Consider using Redis or Redisson for distributed idempotent validation.");
    }

    private final LongSupplier nanoTimeSupplier;
    private final ConcurrentHashMap<String, Record> recordMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isCleaning = new AtomicBoolean(false);
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "velo-idempotent-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Override
    public boolean tryRecord(String key, String token, long timeout) {
        long now = nanoTimeSupplier.getAsLong();
        long ttlNanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        AtomicBoolean success = new AtomicBoolean(false);

        // compute 把"判断是否过期"和"写入新的过期时间"合并成一个原子操作，避免并发穿透。
        recordMap.compute(key, (k, v) -> {
            if (v == null || now - v.createdAtNanos >= v.ttlNanos) {
                success.set(true);
                return new Record(token, now, ttlNanos);
            }
            return v;
        });

        // 过期清理只在 Map 膨胀后异步触发，避免请求主路径反复扫描整个表。
        if (recordMap.mappingCount() > 1024 && isCleaning.compareAndSet(false, true)) {
            try {
                cleanupExecutor.execute(() -> {
                    try {
                        long currentTime = nanoTimeSupplier.getAsLong();
                        recordMap.entrySet().removeIf(entry ->
                                currentTime - entry.getValue().createdAtNanos >= entry.getValue().ttlNanos);
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
        private final long createdAtNanos;
        private final long ttlNanos;

        private Record(String token, long createdAtNanos, long ttlNanos) {
            this.token = token;
            this.createdAtNanos = createdAtNanos;
            this.ttlNanos = ttlNanos;
        }
    }
}
