package io.github.luminion.velo.lock;

/**
 * 锁处理器 SPI
 *
 * @author luminion
 * @since 1.0.0
 */
public interface LockHandler {

    /**
     * 尝试加锁一次，获取不到立即返回，不等待或重试。
     * <p>
     * 锁的生命周期由后端管理：
     * <ul>
     *     <li><b>Redisson</b>：使用原生看门狗续期，直到 {@link #unlock(String)} 释放。</li>
     *     <li><b>Redis</b>：使用构造函数配置的固定 TTL，不续期；业务必须在 TTL 内完成。</li>
     *     <li><b>本地 JDK</b>：仅保证单 JVM 互斥，不自动过期，
     *     靠配对的 {@link #unlock(String)} 释放；没有持有者或正在获取锁的调用时立即回收状态。</li>
     * </ul>
     *
     * <p>Redis 的同线程重入会读取 token 校验持有权，不延长 TTL；
     * 校验发现锁已丢失时重入失败返回 {@code false}。</p>
     *
     * @param key 锁的唯一标识
     * @return true: 加锁成功; false: 加锁失败
     */
    boolean tryLock(String key);

    /**
     * 释放锁
     *
     * @param key 锁的唯一标识
     */
    void unlock(String key);
}
