package io.github.luminion.starter.log.support;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;

/**
 * A MethodLogWriter implementation that uses SLF4J for logging.
 *
 * @author luminion
 */
@Slf4j
public class Slf4JMethodLogWriter extends AbstractMethodLogWriter {
    protected final Level level;

    /**
     * Constructs a new Slf4JMethodLogWriter.
     *
     * @param level The logging level for before and after advice (e.g., Level.INFO,
     *              Level.DEBUG).
     */
    public Slf4JMethodLogWriter(Level level) {
        this.level = level;
    }

    @Override
    public void printMethodArgs(Object target, MethodSignature signature, Object[] args) {
        String methodName = getMethodName(signature);
        String formattedArgs = getFormatArgs(signature.getParameterNames(), args);
        log(level, "==> Enter: {} with args: {}", methodName, formattedArgs);
    }

    @Override
    public void printReturnValue(Object target, MethodSignature signature, Object[] args, Object result) {
        String methodName = getMethodName(signature);
        log(level, "<== Exit: {}. Result: [{}]. Duration: {}ms", methodName, result);
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
