package io.github.luminion.velo.ratelimit.exception;

/**
 * 触发限流时抛出。
 *
 * <p>除提示信息外，还携带限流 key 与窗口配置，便于上层做结构化处理或友好提示。</p>
 */
public class RateLimitException extends RuntimeException {

    private final String key;
    private final double permits;
    private final long window;

    public RateLimitException(String message) {
        this(message, null, 0D, 0L);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
        this.key = null;
        this.permits = 0D;
        this.window = 0L;
    }

    public RateLimitException(String message, String key, double permits, long window) {
        super(message);
        this.key = key;
        this.permits = permits;
        this.window = window;
    }

    /**
     * 触发限流的完整 key（含前缀）。可能为 {@code null}（旧构造方式）。
     */
    public String getKey() {
        return key;
    }

    /**
     * 时间窗口内允许的最大请求数。
     */
    public double getPermits() {
        return permits;
    }

    /**
     * 限流时间窗口大小，单位为毫秒。
     */
    public long getWindow() {
        return window;
    }
}
