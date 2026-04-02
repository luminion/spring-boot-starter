package io.github.luminion.velo.log;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * 接口调用出参记录器
 *
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface InvokeResultWriter {

    /**
     * 记录方法返回值
     *
     * @param signature 方法签名
     * @param result    返回值
     */
    void writeResult(MethodSignature signature, Object result);
}
