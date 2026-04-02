package io.github.luminion.velo.log;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * 异常日志记录器
 *
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface ErrorLogWriter {

    /**
     * 记录异常信息
     *
     * @param signature 方法签名
     * @param args      调用参数 (现场证据)
     * @param e         异常对象
     */
    void writeError(MethodSignature signature, Object[] args, Throwable e);
}
