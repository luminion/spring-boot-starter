package io.github.luminion.velo.log.support;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.StringUtils;

/**
 * SLF4J based unified invocation log writer.
 */
public class Slf4JInvocationLogWriter implements InvocationLogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4JInvocationLogWriter.class);

    private final LogLevel level;

    private final boolean includeErrorStackTrace;

    public Slf4JInvocationLogWriter(VeloProperties properties) {
        this.level = properties.getLog().getLevel();
        this.includeErrorStackTrace = properties.getLog().getInvocation().isIncludeErrorStackTrace();
    }

    @Override
    public void write(InvocationLogRecord record) {
        if (record == null) {
            return;
        }
        if (level == LogLevel.OFF) {
            return;
        }
        if (!record.isSuccess()) {
            String message = buildMessage(record);
            if (includeErrorStackTrace && record.getError() != null) {
                resolveLevel(LogLevel.ERROR).logError(message, record.getError());
            } else {
                resolveLevel(LogLevel.ERROR).log(message);
            }
            return;
        }
        LogOperations ops = resolveLevel(level);
        if (!ops.isEnabled()) {
            return;
        }
        ops.log(buildMessage(record));
    }

    private String buildMessage(InvocationLogRecord record) {
        StringBuilder builder = new StringBuilder();
        append(builder, "traceId", text(record.getTraceId()));
        append(builder, "source", source(record.getSource()));
        appendQuoted(builder, "target", text(record.getTarget()));
        append(builder, "cost", record.getCostMs() + "ms");
        append(builder, "status", record.isSuccess() ? "success" : "error");
        if (record.isSlow()) {
            append(builder, "slow", "true");
        }
        append(builder, "args", text(record.getArgs()));
        if (record.isSuccess()) {
            append(builder, "result", text(record.getResult()));
        } else {
            appendQuoted(builder, "error", InvocationLogSupport.errorSummary(record.getError()));
        }
        return builder.toString();
    }

    private String source(InvocationLogSource source) {
        return source == null ? InvocationLogSupport.EMPTY_PAYLOAD : source.getValue();
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value : InvocationLogSupport.EMPTY_PAYLOAD;
    }

    private void append(StringBuilder builder, String key, String value) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(key).append('=').append(value);
    }

    private void appendQuoted(StringBuilder builder, String key, String value) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(key).append("=\"").append(value).append('"');
    }

    private static LogOperations resolveLevel(LogLevel level) {
        LogLevel target = level == null ? LogLevel.INFO : level;
        switch (target) {
            case TRACE: return LogOperations.TRACE;
            case DEBUG: return LogOperations.DEBUG;
            case WARN:  return LogOperations.WARN;
            case ERROR:
            case FATAL: return LogOperations.ERROR;
            default:    return LogOperations.INFO;
        }
    }

    private enum LogOperations {
        TRACE {
            @Override boolean isEnabled() { return LOGGER.isTraceEnabled(); }
            @Override void log(String msg) { LOGGER.trace(msg); }
            @Override void logError(String msg, Throwable t) { LOGGER.trace(msg, t); }
        },
        DEBUG {
            @Override boolean isEnabled() { return LOGGER.isDebugEnabled(); }
            @Override void log(String msg) { LOGGER.debug(msg); }
            @Override void logError(String msg, Throwable t) { LOGGER.debug(msg, t); }
        },
        INFO {
            @Override boolean isEnabled() { return LOGGER.isInfoEnabled(); }
            @Override void log(String msg) { LOGGER.info(msg); }
            @Override void logError(String msg, Throwable t) { LOGGER.info(msg, t); }
        },
        WARN {
            @Override boolean isEnabled() { return LOGGER.isWarnEnabled(); }
            @Override void log(String msg) { LOGGER.warn(msg); }
            @Override void logError(String msg, Throwable t) { LOGGER.warn(msg, t); }
        },
        ERROR {
            @Override boolean isEnabled() { return LOGGER.isErrorEnabled(); }
            @Override void log(String msg) { LOGGER.error(msg); }
            @Override void logError(String msg, Throwable t) { LOGGER.error(msg, t); }
        };

        abstract boolean isEnabled();
        abstract void log(String msg);
        abstract void logError(String msg, Throwable t);
    }
}
