package io.github.luminion.velo.idempotent.exception;

import java.util.concurrent.TimeUnit;

/**
 * 幂等校验未通过（窗口内重复提交）时抛出。
 *
 * <p>除提示信息外，还携带触发幂等的 key 与窗口配置，便于上层做结构化处理或友好提示。</p>
 */
public class IdempotentException extends RuntimeException {

    private final String key;
    private final long ttl;
    private final TimeUnit unit;

    public IdempotentException(String message) {
        this(message, null, 0L, null);
    }

    public IdempotentException(String message, String key, long ttl, TimeUnit unit) {
        super(message);
        this.key = key;
        this.ttl = ttl;
        this.unit = unit;
    }

    /**
     * 触发幂等的完整 key（含前缀）。可能为 {@code null}（旧构造方式）。
     */
    public String getKey() {
        return key;
    }

    /**
     * 幂等窗口时长。
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * 幂等窗口时间单位。可能为 {@code null}（旧构造方式）。
     */
    public TimeUnit getUnit() {
        return unit;
    }
}
