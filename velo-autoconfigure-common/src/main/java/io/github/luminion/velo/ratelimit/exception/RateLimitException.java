package io.github.luminion.velo.ratelimit.exception;

/**
 * 触发限流时抛出。
 *
 * <p>除提示信息外，还携带限流 key 与 QPS 配置，便于上层做结构化处理或友好提示。</p>
 */
public class RateLimitException extends RuntimeException {

    private final String key;
    private final int qps;

    public RateLimitException(String message) {
        this(message, null, 0);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
        this.key = null;
        this.qps = 0;
    }

    public RateLimitException(String message, String key, int qps) {
        super(message);
        this.key = key;
        this.qps = qps;
    }

    /**
     * 触发限流的完整 key（含前缀）。可能为 {@code null}（旧构造方式）。
     */
    public String getKey() {
        return key;
    }

    /**
     * 每秒请求速率。
     */
    public int getQps() {
        return qps;
    }
}
