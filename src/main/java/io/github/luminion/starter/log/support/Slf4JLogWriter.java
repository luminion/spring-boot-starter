package io.github.luminion.starter.log.support;

import io.github.luminion.starter.core.util.AspectUtils;
import io.github.luminion.starter.log.InvokeArgsWriter;
import io.github.luminion.starter.log.InvokeResultWriter;
import io.github.luminion.starter.log.SlowLogWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;

/**
 * 基于 SLF4J 的默认日志写入实现
 *
 * @author luminion
 */
@Slf4j
public class Slf4JLogWriter implements InvokeArgsWriter, InvokeResultWriter, SlowLogWriter {
    protected final Level level;

    public Slf4JLogWriter(Level level) {
        this.level = level;
    }

    @Override
    public void writeArgs(MethodSignature signature, Object[] args) {
        String methodName = AspectUtils.getMethodName(signature);
        String formattedArgs = AspectUtils.getFormatArgs(signature, args);
        log(level, "==> Enter: {} with args: {}", methodName, formattedArgs);
    }

    @Override
    public void writeResult(MethodSignature signature, Object result) {
        String methodName = AspectUtils.getMethodName(signature);
        log(level, "<== Exit: {}. Result: [{}]", methodName, result);
    }

    @Override
    public void writeSlow(MethodSignature signature, long durationNs) {
        String methodName = AspectUtils.getMethodName(signature);
        log(level, "!!> Slow Log: {} => Time Cost: {}ms", methodName, durationNs/1000);
    }

    protected void log(Level level, String format, Object... arguments) {
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
        }
    }
}
