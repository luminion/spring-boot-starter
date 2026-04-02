package io.github.luminion.velo.lock;

import java.util.concurrent.TimeUnit;

/**
 * 锁处理器 SPI
 *
 * @author luminion
 * @since 1.0.0
 */
public interface LockHandler {

    /**
     * 尝试加锁
     *
     * @param key       锁的唯一标识
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return true: 加锁成功; false: 加锁失败
     */
    boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁
     *
     * @param key 锁的唯一标识
     */
    void unlock(String key);
}
