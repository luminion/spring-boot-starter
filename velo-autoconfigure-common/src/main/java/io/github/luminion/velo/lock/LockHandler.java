package io.github.luminion.velo.lock;

/**
 * 锁处理器 SPI
 *
 * @author luminion
 * @since 1.0.0
 */
public interface LockHandler {

    /**
     * 尝试加锁。
     * <p>
     * {@code leaseTime} 是能力依赖后端的参数，各实现按自身能力处理：
     * <ul>
     *     <li><b>Redisson</b>：{@code >0} 为固定租约到期释放；{@code -1} 启用看门狗，
     *     在业务执行期间自动续约，直到 {@link #unlock(String)} 释放。</li>
     *     <li><b>Redis</b>（基于 setIfAbsent 的简单实现）：{@code >0} 为固定 TTL 到期释放；
     *     不支持看门狗，收到 {@code -1} 时降级为固定默认租约并打印告警。</li>
     *     <li><b>本地 JDK / Caffeine</b>：仅保证单 JVM 互斥，忽略 {@code leaseTime}，
     *     锁不自动过期，靠配对的 {@link #unlock(String)} 释放（由引用计数清理，无内存泄漏）。</li>
     * </ul>
     *
     * @param key       锁的唯一标识
     * @param waitTime  等待时间，单位为毫秒
     * @param leaseTime 持有时间，单位为毫秒；{@code -1} 表示请求看门狗式自动续约，具体行为见上方各后端说明
     * @return true: 加锁成功; false: 加锁失败
     */
    boolean lock(String key, long waitTime, long leaseTime);

    /**
     * 释放锁
     *
     * @param key 锁的唯一标识
     */
    void unlock(String key);
}
