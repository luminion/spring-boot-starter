package io.github.luminion.starter.log;

import org.aspectj.lang.reflect.MethodSignature;

/**
 * @author luminion
 */
public interface InvokeLogWriter {

    void printMethodArgs(Object target, MethodSignature signature, Object[] args);

    void printReturnValue(Object target, MethodSignature signature, Object[] args, Object result, long duration);

}
