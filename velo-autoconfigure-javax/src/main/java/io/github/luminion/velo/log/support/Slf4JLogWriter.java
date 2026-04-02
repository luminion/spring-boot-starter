package io.github.luminion.velo.log.support;

import io.github.luminion.velo.core.util.InvocationUtils;
import io.github.luminion.velo.log.ErrorLogWriter;
import io.github.luminion.velo.log.InvokeArgsWriter;
import io.github.luminion.velo.log.InvokeResultWriter;
import io.github.luminion.velo.log.SlowLogWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;

/**
 * 基于 SLF4J 的默认日志写入实现
 *
 * @author luminion
 */
@Slf4j
public class Slf4JLogWriter implements InvokeArgsWriter, InvokeResultWriter, SlowLogWriter, ErrorLogWriter {
    protected final Level level;

    public Slf4JLogWriter(Level level) {
        this.level = level;
    }

    @Override
    public void writeArgs(MethodSignature signature, Object[] args) {
        String methodName = InvocationUtils.getMethodName(signature);
        String formattedArgs = InvocationUtils.formatArguments(signature, args, 2000);
        log(level, "==> Enter: {} with args: {}", methodName, formattedArgs);
    }

    @Override
    public void writeResult(MethodSignature signature, Object result) {
        String methodName = InvocationUtils.getMethodName(signature);
        log(level, "<== Exit: {}. Result: [{}]", methodName, result);
    }

    @Override
    public void writeSlow(MethodSignature signature, long durationNs) {
        String methodName = InvocationUtils.getMethodName(signature);
        log(level, "!!> Slow Log: {} => Time Cost: {}ms", methodName, durationNs / 1_000_000);
    }

    @Override
    public void writeError(MethodSignature signature, Object[] args, Throwable e) {
        if (level == null) {
            return;
        }
        String methodName = InvocationUtils.getMethodName(signature);
        String formattedArgs = InvocationUtils.formatArguments(signature, args, 2000);
        log(Level.ERROR, "> Error: {} with args: {}. Exception: {}", methodName, formattedArgs, e.getMessage(), e);
    }

    protected void log(Level level, String format, Object... arguments) {
        if (level == null) {
            return;
        }
        switch (level) {
            case INFO:
                log.info(format, arguments);
                break;
            case DEBUG:
                log.debug(format, arguments);
                break;
            case WARN:
                log.warn(format, arguments);
                break;
            case TRACE:
                log.trace(format, arguments);
                break;
            case ERROR:
                log.error(format, arguments);
                break;
            default:
                break;
        }
    }
}
