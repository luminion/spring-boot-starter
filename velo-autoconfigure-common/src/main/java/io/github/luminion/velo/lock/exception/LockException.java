package io.github.luminion.velo.lock.exception;

import lombok.Getter;

/**
 * 获取锁失败时抛出。
 *
 * <p>除提示信息外，还携带锁 key，便于上层做结构化处理或友好提示。</p>
 */
@Getter
public class LockException extends RuntimeException {

    /**
     * -- GETTER --
     *  获取失败的锁的完整 key（含前缀）。可能为
     * （旧构造方式）。
     */
    private final String key;

    public LockException(String message) {
        this(message, null);
    }

    public LockException(String message, String key) {
        super(message);
        this.key = key;
    }

}
