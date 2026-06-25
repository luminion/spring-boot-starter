package io.github.luminion.velo.idempotent;

import java.util.concurrent.TimeUnit;

/**
 * 幂等执行器 SPI
 *
 * @author luminion
 */
public interface IdempotentHandler {

    /**
     * 尝试记录该幂等 Key，并绑定一个唯一 token。
     * <p>
     * token 用于标识"本次请求"，便于在业务失败后只清除自己写入的记录，
     * 避免并发场景下误删其他请求刚写入的幂等记录。
     *
     * @param key     唯一键
     * @param token   本次请求的唯一标识（通常为 UUID）
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true: 记录成功（首次进入）; false: 记录失败（重复请求）
     */
    boolean tryRecord(String key, String token, long timeout, TimeUnit unit);

    /**
     * 仅当存储的 token 与传入 token 一致时，才移除该幂等记录。
     * <p>
     * 用于在业务失败后允许重新提交。token 比对保证只清除本次请求写入的记录，
     * 不会误删并发请求在窗口内刚写入的新记录。
     *
     * @param key   唯一键
     * @param token 本次请求的唯一标识
     */
    default void removeIfMatch(String key, String token) {
        // no-op by default, implementations should override to provide cleanup
    }
}
