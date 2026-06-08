package io.github.luminion.velo.idempotent;

import java.util.concurrent.TimeUnit;

/**
 * 幂等执行器 SPI
 *
 * @author luminion
 */
public interface IdempotentHandler {

    /**
     * 尝试记录该幂等 Key。
     *
     * @param key     唯一键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true: 记录成功（首次进入）; false: 记录失败（重复请求）
     */
    boolean tryRecord(String key, long timeout, TimeUnit unit);

    /**
     * 移除已记录的幂等 Key，允许在业务失败后重新提交。
     *
     * @param key 唯一键
     */
    default void remove(String key) {
        // no-op by default, implementations should override to provide cleanup
    }
}
