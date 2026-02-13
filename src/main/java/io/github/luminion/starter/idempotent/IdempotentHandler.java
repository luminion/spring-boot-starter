package io.github.luminion.starter.idempotent;

import java.util.concurrent.TimeUnit;

/**
 * 幂等执行器 SPI
 *
 * @author luminion
 */
public interface IdempotentHandler {

    /**
     * 尝试锁定/记录该幂等 Key
     *
     * @param key     唯一键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true: 锁定成功（首次进入）; false: 锁定失败（重复请求）
     */
    boolean tryLock(String key, long timeout, TimeUnit unit);

    /**
     * 释放该幂等 Key
     *
     * @param key 唯一键
     */
    void release(String key);

}
