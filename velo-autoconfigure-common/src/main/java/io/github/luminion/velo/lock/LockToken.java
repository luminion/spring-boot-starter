package io.github.luminion.velo.lock;

import java.util.Objects;

/**
 * 锁持有令牌。
 *
 * <p>响应式执行可能在不同线程上完成，不能依赖当前线程推断锁所有权。令牌携带后端需要的
 * 所有权信息，由 {@link ReactiveLockHandler} 在任意 Reactor 信号线程上完成释放。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
public final class LockToken {

    private final String key;

    private final Object owner;

    public LockToken(String key, Object owner) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    public String getKey() {
        return key;
    }

    public Object getOwner() {
        return owner;
    }
}
