package io.github.luminion.starter.log;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * 慢日志记录器
 *
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface SlowLogWriter {

    /**
     * 记录慢日志
     *
     * @param signature 方法签名
     * @param durationNs  实际耗时(nanoseconds)
     */
    void writeSlow(MethodSignature signature, long durationNs);
}
