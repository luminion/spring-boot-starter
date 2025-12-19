package io.github.luminion.autoconfigure.aop.core;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * @author luminion
 */
public interface OperationLogWriter {

    void before(Object target, MethodSignature signature, Object[] args);

    void after(Object target, MethodSignature signature, Object[] args, Object result, long duration);

    void error(Object target, MethodSignature signature, Object[] args, Throwable throwable, long duration);
}
