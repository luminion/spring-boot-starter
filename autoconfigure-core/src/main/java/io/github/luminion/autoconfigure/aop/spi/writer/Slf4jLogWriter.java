package io.github.luminion.autoconfigure.aop.spi.writer;

import io.github.luminion.autoconfigure.aop.spi.LogWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;

import java.util.stream.IntStream;

/**
 * A LogWriter implementation that uses SLF4J for logging.
 *
 * @author luminion
 */
@Slf4j
public class Slf4jLogWriter implements LogWriter {
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
        String formattedArgs = formatArgs(signature.getParameterNames(), args);
        log(level, "==> Enter: {} with args: {}", methodName, formattedArgs);
    }

    @Override
    public void after(Object target, MethodSignature signature, Object[] args, Object result, long duration) {
        String methodName = getMethodName(signature);
        log(level, "<== Exit: {}. Result: [{}]. Duration: {}ms", methodName, result, duration);
    }

    @Override
    public void error(Object target, MethodSignature signature, Object[] args, Throwable throwable, long duration) {
        String methodName = getMethodName(signature);
        String formattedArgs = formatArgs(signature.getParameterNames(), args);
        log.error("<== Exception in {} with args: {}. Duration: {}ms", methodName, formattedArgs, duration, throwable);
    }

    protected String getMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    protected String formatArgs(String[] paramNames, Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        IntStream.range(0, args.length).forEach(i -> {
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            sb.append(paramName).append("=").append(args[i]);
            if (i < args.length - 1) {
                sb.append(", ");
            }
        });
        sb.append("]");
        return sb.toString();
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
