package io.github.luminion.velo.lock.exception;

import java.util.concurrent.TimeUnit;

/**
 * 获取锁失败时抛出。
 *
 * <p>除提示信息外，还携带锁 key 与等待/持有配置，便于上层做结构化处理或友好提示。</p>
 */
public class LockException extends RuntimeException {

    private final String key;
    private final long waitTimeout;
    private final long lease;
    private final TimeUnit unit;

    public LockException(String message) {
        this(message, null, 0L, 0L, null);
    }

    public LockException(String message, String key, long waitTimeout, long lease, TimeUnit unit) {
        super(message);
        this.key = key;
        this.waitTimeout = waitTimeout;
        this.lease = lease;
        this.unit = unit;
    }

    /**
     * 获取失败的锁的完整 key（含前缀）。可能为 {@code null}（旧构造方式）。
     */
    public String getKey() {
        return key;
    }

    /**
     * 等待获取锁的时间。
     */
    public long getWaitTimeout() {
        return waitTimeout;
    }

    /**
     * 锁的持有时间。
     */
    public long getLease() {
        return lease;
    }

    /**
     * 时间单位。可能为 {@code null}（旧构造方式）。
     */
    public TimeUnit getUnit() {
        return unit;
    }
}
