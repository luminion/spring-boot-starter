package io.github.luminion.starter.log;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * @author luminion
 */
public interface LogWriter {

    void before(Object target, MethodSignature signature, Object[] args);

    void after(Object target, MethodSignature signature, Object[] args, Object result);

    void error(Object target, MethodSignature signature, Object[] args, Throwable throwable);
}
