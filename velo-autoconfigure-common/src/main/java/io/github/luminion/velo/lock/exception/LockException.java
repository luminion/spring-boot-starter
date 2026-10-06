package io.github.luminion.velo.lock.exception;

/**
 * 获取锁失败时抛出。
 *
 * <p>除提示信息外，还携带锁 key，便于上层做结构化处理或友好提示。</p>
 */
public class LockException extends RuntimeException {

    private final String key;

    public LockException(String message) {
        this(message, null);
    }

    public LockException(String message, String key) {
        super(message);
        this.key = key;
    }

    /**
     * 获取失败的锁的完整 key（含前缀）。可能为 {@code null}（旧构造方式）。
     */
    public String getKey() {
        return key;
    }

}
