package io.github.luminion.velo.idempotent.exception;

/**
 * 幂等校验未通过（窗口内重复提交）时抛出。
 *
 * <p>除提示信息外，还携带触发幂等的 key 与窗口配置，便于上层做结构化处理或友好提示。</p>
 */
public class IdempotentException extends RuntimeException {

    private final String key;
    private final long ttl;

    public IdempotentException(String message) {
        this(message, null, 0L);
    }

    public IdempotentException(String message, String key, long ttl) {
        super(message);
        this.key = key;
        this.ttl = ttl;
    }

    /**
     * 触发幂等的完整 key（含前缀）。可能为 {@code null}（旧构造方式）。
     */
    public String getKey() {
        return key;
    }

    /**
     * 幂等窗口时长，单位为毫秒。
     */
    public long getTtl() {
        return ttl;
    }

}
