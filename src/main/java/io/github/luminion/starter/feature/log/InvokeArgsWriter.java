package io.github.luminion.starter.feature.log;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * 接口调用入参记录器
 *
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface InvokeArgsWriter {

    /**
     * 记录方法入参
     *
     * @param signature 方法签名
     * @param args      参数列表
     */
    void writeArgs(MethodSignature signature, Object[] args);
}
