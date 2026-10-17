package io.github.luminion.velo.log.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * 单项功能的完整策略；缓存后不可变，不包含继承状态。
 */
@Getter
public final class InvocationLogPolicy {
    /**
     * 实际日志级别。
     */
    private final LogPolicyLevel level;
    /**
     * 慢调用阈值，单位毫秒。
     */
    private final long threshold;
    /**
     * 头名称白名单，空列表表示全部。
     */
    private final List<String> allowlist;
    /**
     * 正文长度上限，-1 不限，0 关闭正文日志。
     */
    private final int maxLength;

    public InvocationLogPolicy(LogPolicyLevel level, long threshold, List<String> allowlist, int maxLength) {
        if (threshold < 0) {
            throw new IllegalArgumentException("SlowLog threshold must be zero or greater");
        }
        if (maxLength < -1) {
            throw new IllegalArgumentException("Log maxLength must be -1 or zero or greater");
        }
        this.level = Objects.requireNonNull(level, "level");
        this.threshold = threshold;
        this.allowlist = Collections.unmodifiableList(new ArrayList<>(allowlist));
        this.maxLength = maxLength;
    }
}
