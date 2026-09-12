package io.github.luminion.velo.lock;

import java.util.concurrent.CompletionStage;

/**
 * 支持响应式线程切换的锁处理器 SPI。
 *
 * <p>普通 {@link LockHandler} 的 {@code unlock(String)} 通常依赖当前线程，不能直接用于
 * WebFlux。实现该接口后，锁的获取和释放通过 {@link LockToken} 绑定所有权，不要求两次操作
 * 运行在同一线程。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
public interface ReactiveLockHandler extends LockHandler {

    /**
     * 使用独立所有权令牌尝试加锁。
     *
     * @param key       锁的唯一标识
     * @param waitTime  等待时间，单位为毫秒
     * @param leaseTime 持有时间，单位为毫秒
     * @return 获取成功时返回令牌，获取失败时返回 {@code null}
     */
    CompletionStage<LockToken> lockToken(String key, long waitTime, long leaseTime);

    /**
     * 使用令牌释放锁。
     *
     * @param token 加锁时返回的令牌
     */
    CompletionStage<Void> unlockToken(LockToken token);
}
