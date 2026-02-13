package io.github.luminion.starter.log.support;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;

/**
 * A LogWriter implementation that uses SLF4J for logging.
 *
 * @author luminion
 */
@Slf4j
public class Slf4jLogWriter extends AbstractLogWriter {
    protected final Level level;

    /**
     * Constructs a new Slf4jLogWriter.
     *
     * @param level The logging level for before and after advice (e.g., Level.INFO, Level.DEBUG).
     */
    public Slf4jLogWriter(Level level) {
        this.level = level;
    }

    @Override
    public void before(Object target, MethodSignature signature, Object[] args) {
        String methodName = getMethodName(signature);
        String formattedArgs = getFormatArgs(signature.getParameterNames(), args);
        log(level, "==> Enter: {} with args: {}", methodName, formattedArgs);
    }

    @Override
    public void after(Object target, MethodSignature signature, Object[] args, Object result) {
        String methodName = getMethodName(signature);
        log(level, "<== Exit: {}. Result: [{}]. Duration: {}ms", methodName, result);
    }

    @Override
    public void error(Object target, MethodSignature signature, Object[] args, Throwable throwable) {
        String methodName = getMethodName(signature);
        String formattedArgs = getFormatArgs(signature.getParameterNames(), args);
        log.error("<== Exception in {} with args: {}", methodName, formattedArgs, throwable);
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
