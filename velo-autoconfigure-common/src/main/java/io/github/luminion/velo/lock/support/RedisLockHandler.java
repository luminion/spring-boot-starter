package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockToken;
import io.github.luminion.velo.lock.ReactiveLockHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Redis 的分布式锁实现。
 *
 * <p>支持同线程可重入：按线程以栈结构记录持有情况，同一线程对同一 key 再次加锁时只在本地增加持有计数、
 * 不再访问 Redis，避免自己等待自己导致的死锁；只有最外层 unlock 才真正删除 Redis 锁。
 *
 * <p>正数 lease 保持 Redis 固定 TTL 语义；lease=-1 使用本实现的看门狗，在业务执行期间定期续约。
 * 看门狗只保证进程仍能执行续期任务时的长任务持有，进程崩溃后 Redis TTL 仍会自然过期。
 */
@Slf4j
public class RedisLockHandler implements ReactiveLockHandler, AutoCloseable {

    private static final String RELEASE_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";
    private static final String RENEW_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "else return 0 end";

    // 脚本无状态，提取为静态常量复用，避免每次 unlock 重复构建
    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(RELEASE_LUA, Long.class);
    private static final RedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(RENEW_LUA, Long.class);

    // 看门狗使用 30 秒 TTL，每 10 秒续期一次；进程崩溃后最多等待一个 TTL 即可释放。
    private static final long WATCHDOG_LEASE_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final long WATCHDOG_RENEW_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(10);

    private final StringRedisTemplate redisTemplate;
    private final long retryIntervalNanos;
    private final ThreadLocal<Map<String, Deque<String>>> lockValues = new ThreadLocal<>();
    private final ScheduledExecutorService watchdogExecutor;
    private final ConcurrentMap<String, WatchdogRegistration> watchdogs = new ConcurrentHashMap<>();

    public RedisLockHandler(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_RETRY_INTERVAL);
    }

    public RedisLockHandler(StringRedisTemplate redisTemplate, Duration retryInterval) {
        this(redisTemplate, retryInterval, newWatchdogExecutor());
    }

    RedisLockHandler(StringRedisTemplate redisTemplate, Duration retryInterval,
            ScheduledExecutorService watchdogExecutor) {
        if (retryInterval == null || retryInterval.isZero() || retryInterval.isNegative()) {
            throw new IllegalArgumentException("Redis lock retry interval must be greater than zero.");
        }
        if (redisTemplate == null) {
            throw new IllegalArgumentException("Redis lock template must not be null.");
        }
        if (watchdogExecutor == null) {
            throw new IllegalArgumentException("Redis lock watchdog executor must not be null.");
        }
        this.redisTemplate = redisTemplate;
        this.retryIntervalNanos = retryInterval.toNanos();
        this.watchdogExecutor = watchdogExecutor;
    }

    @Override
    public boolean lock(String key, long waitTime, long leaseTime) {
        Map<String, Deque<String>> values = lockValues.get();
        Deque<String> stack = values == null ? null : values.get(key);
        // 同线程已持有该 key：本地重入，仅增加持有计数(压入同一 owner token)，不再访问 Redis
        if (stack != null && !stack.isEmpty()) {
            stack.push(stack.peek());
            return true;
        }

        String lockValue = acquireRedisLock(key, waitTime, leaseTime);
        if (lockValue == null) {
            return false;
        }

        try {
            startWatchdogIfRequested(key, lockValue, leaseTime);
        } catch (RuntimeException e) {
            releaseRedisLock(key, lockValue);
            throw e;
        }

        // 首次获取成功，记录 owner token，解锁时用它和 Redis 当前值比对，避免误删已续租或被他人重建的锁。
        if (values == null) {
            values = new HashMap<>();
            lockValues.set(values);
        }
        values.computeIfAbsent(key, k -> new ArrayDeque<>()).push(lockValue);
        return true;
    }

    @Override
    public void unlock(String key) {
        Map<String, Deque<String>> values = lockValues.get();
        if (values == null) {
            return;
        }
        Deque<String> stack = values.get(key);
        if (stack == null || stack.isEmpty()) {
            if (values.isEmpty()) {
                lockValues.remove();
            }
            return;
        }

        String lockValue = stack.pop();
        boolean outermost = stack.isEmpty();
        if (outermost) {
            values.remove(key);
        }

        try {
            // 仅最外层释放时才真正删除 Redis 锁；重入的内层释放只递减本地持有计数。
            // 删除动作必须和 token 校验放在同一个 Lua 脚本里，才能保证"检查后删除"是原子的。
            if (outermost) {
                cancelWatchdog(lockValue);
                releaseRedisLock(key, lockValue);
            }
        } catch (Exception e) {
            log.warn("Redis unlock failed for key: {}", key, e);
        } finally {
            if (values.isEmpty()) {
                lockValues.remove();
            }
        }
    }

    @Override
    public CompletionStage<LockToken> lockToken(String key, long waitTime, long leaseTime) {
        String lockValue = acquireRedisLock(key, waitTime, leaseTime);
        if (lockValue != null) {
            try {
                startWatchdogIfRequested(key, lockValue, leaseTime);
            } catch (RuntimeException e) {
                releaseRedisLock(key, lockValue);
                throw e;
            }
        }
        return CompletableFuture.completedFuture(lockValue == null ? null : new LockToken(key, lockValue));
    }

    @Override
    public CompletionStage<Void> unlockToken(LockToken token) {
        if (token == null || !(token.getOwner() instanceof String)) {
            return CompletableFuture.completedFuture(null);
        }
        String lockValue = (String) token.getOwner();
        cancelWatchdog(lockValue);
        releaseRedisLock(token.getKey(), lockValue);
        return CompletableFuture.completedFuture(null);
    }

    private String acquireRedisLock(String key, long waitTime, long leaseTime) {
        long waitNanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
        long leaseMillis = resolveLeaseMillis(leaseTime);
        long startNanos = System.nanoTime();

        while (true) {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, lockValue, leaseMillis, TimeUnit.MILLISECONDS);
            if (success == null) {
                // setIfAbsent 返回 null 说明命令被 Redis 事务/pipeline 排队而非立即执行，加锁判定失效。
                // 分布式锁不应包裹在 Redis 事务里，打 WARN 提示误用。
                log.warn("[Velo Starter] Redis lock setIfAbsent returned null for key '{}'. " +
                        "This usually means the operation is wrapped in a Redis transaction/pipeline, " +
                        "which defers execution and breaks locking. Avoid acquiring locks inside a Redis transaction.", key);
            }
            if (Boolean.TRUE.equals(success)) {
                return lockValue;
            }

            if (waitNanos <= 0L) {
                return null;
            }

            long remainingNanos = waitNanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0L) {
                return null;
            }
            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(remainingNanos, retryIntervalNanos));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    private long resolveLeaseMillis(long leaseTime) {
        // lease=-1 使用看门狗，但 Redis 初始值仍必须有 TTL，避免进程崩溃后锁永久残留。
        return leaseTime < 0L ? WATCHDOG_LEASE_MILLIS : leaseTime;
    }

    private void startWatchdogIfRequested(String key, String lockValue, long leaseTime) {
        if (leaseTime != -1L) {
            return;
        }

        WatchdogRegistration watchdog = new WatchdogRegistration(key, lockValue);
        watchdogs.put(lockValue, watchdog);
        try {
            ScheduledFuture<?> future = watchdogExecutor.scheduleAtFixedRate(watchdog,
                    WATCHDOG_RENEW_INTERVAL_MILLIS, WATCHDOG_RENEW_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            watchdog.setFuture(future);
        } catch (RuntimeException e) {
            watchdogs.remove(lockValue, watchdog);
            watchdog.cancel();
            throw new IllegalStateException("Unable to start Redis lock watchdog.", e);
        }
    }

    private void cancelWatchdog(String lockValue) {
        WatchdogRegistration watchdog = watchdogs.remove(lockValue);
        if (watchdog != null) {
            watchdog.cancel();
        }
    }

    private void releaseRedisLock(String key, String lockValue) {
        try {
            // 仅当 token 匹配时删除，避免误删其他请求已经重新获取的锁。
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), lockValue);
        } catch (Exception e) {
            log.warn("Redis unlock failed for key: {}", key, e);
        }
    }

    private static ScheduledExecutorService newWatchdogExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "velo-redis-lock-watchdog");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void close() {
        watchdogs.values().forEach(WatchdogRegistration::cancel);
        watchdogs.clear();
        watchdogExecutor.shutdownNow();
    }

    private final class WatchdogRegistration implements Runnable {

        private final String key;
        private final String lockValue;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final AtomicBoolean renewalFailureLogged = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> future;

        private WatchdogRegistration(String key, String lockValue) {
            this.key = key;
            this.lockValue = lockValue;
        }

        private void setFuture(ScheduledFuture<?> future) {
            this.future = future;
            if (!active.get()) {
                future.cancel(false);
            }
        }

        @Override
        public void run() {
            if (!active.get()) {
                return;
            }
            try {
                Long result = redisTemplate.execute(RENEW_SCRIPT, Collections.singletonList(key), lockValue,
                        Long.toString(WATCHDOG_LEASE_MILLIS));
                if (!Long.valueOf(1L).equals(result)) {
                    stop("the lock is no longer owned");
                }
            } catch (Exception e) {
                // 单次网络抖动不立即放弃续约；只要 Redis 后续恢复，token 校验仍能避免续到别人的锁。
                if (renewalFailureLogged.compareAndSet(false, true)) {
                    log.warn("Redis lock watchdog renewal failed for key '{}'; will retry while the lock exists.", key,
                            e);
                }
            }
        }

        private void stop(String reason) {
            if (!active.compareAndSet(true, false)) {
                return;
            }
            watchdogs.remove(lockValue, this);
            ScheduledFuture<?> scheduledFuture = future;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            log.warn("Redis lock watchdog stopped for key '{}': {}.", key, reason);
        }

        private void cancel() {
            if (!active.compareAndSet(true, false)) {
                return;
            }
            watchdogs.remove(lockValue, this);
            ScheduledFuture<?> scheduledFuture = future;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        }
    }

}
