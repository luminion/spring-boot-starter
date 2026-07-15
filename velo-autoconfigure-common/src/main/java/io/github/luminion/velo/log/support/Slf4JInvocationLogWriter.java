package io.github.luminion.velo.log.support;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SLF4J based unified invocation log writer.
 *
 * <p>写入规则：
 * <ul>
 *   <li>慢日志记录（{@code isSlow=true}）：由 {@code velo.log.slow.level} 控制（默认 WARN）</li>
 *   <li>普通调用记录：由 {@code velo.log.level} 控制（默认 INFO）</li>
 *   <li>已启用的异常记录（{@code !isSuccess}）：提升到 ERROR 级别</li>
 * </ul>
 *
 * <p>格式规则：
 * <ul>
 *   <li>{@link InvocationPhase#ENTRY}：{@code [target] ==> args=...}</li>
 *   <li>{@link InvocationPhase#EXIT}：{@code [target] <== cost=Xms result=...}</li>
 *   <li>phase 为 null（慢日志等）：{@code [target] cost=Xms threshold=Yms args=... result=...}</li>
 * </ul>
 */
public class Slf4JInvocationLogWriter implements InvocationLogWriter {

    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(Slf4JInvocationLogWriter.class);

    private final ConcurrentMap<String, Logger> loggerCache = new ConcurrentHashMap<>();

    private final LogLevel level;

    private final LogLevel slowLevel;

    private final boolean includeErrorStackTrace;

    public Slf4JInvocationLogWriter(VeloProperties properties) {
        this.level = properties.getLog().getLevel();
        this.slowLevel = properties.getLog().getSlow().getLevel();
        this.includeErrorStackTrace = properties.getLog().getInvocation().isIncludeErrorStackTrace();
    }

    @Override
    public void write(InvocationLogRecord record) {
        if (record == null) {
            return;
        }
        LogLevel configuredLevel = record.isSlow() ? slowLevel : level;
        if (configuredLevel == LogLevel.OFF) {
            return;
        }

        if (!record.isSuccess()) {
            String message = buildMessage(record);
            Logger logger = resolveLogger(record.getLoggerName());
            if (includeErrorStackTrace && record.getError() != null) {
                resolveLevel(LogLevel.ERROR).logError(logger, message, record.getError());
            } else {
                resolveLevel(LogLevel.ERROR).log(logger, message);
            }
            return;
        }

        LogOperations ops = resolveLevel(configuredLevel);
        Logger logger = resolveLogger(record.getLoggerName());
        if (!ops.isEnabled(logger)) {
            return;
        }
        ops.log(logger, buildMessage(record));
    }

    private String buildMessage(InvocationLogRecord record) {
        InvocationPhase phase = record.getPhase();
        if (phase == InvocationPhase.ENTRY) {
            return buildEntryMessage(record);
        }
        if (phase == InvocationPhase.EXIT) {
            return buildExitMessage(record);
        }
        // phase 为 null：旧单行格式（慢日志使用）
        return buildSingleLineMessage(record);
    }

    /** 进入阶段：{@code [target] ==> args=...} */
    private String buildEntryMessage(InvocationLogRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(text(record.getTarget())).append(']');
        builder.append(" ==>");
        append(builder, "args", text(record.getArgs()));
        return builder.toString();
    }

    /** 退出阶段：{@code [target] <== cost=Xms result=...} 或 {@code <== cost=Xms error="..."} */
    private String buildExitMessage(InvocationLogRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(text(record.getTarget())).append(']');
        builder.append(" <==");
        append(builder, "cost", record.getCostMs() + "ms");
        if (record.isSuccess()) {
            append(builder, "result", text(record.getResult()));
        } else {
            appendQuoted(builder, "error", InvocationLogSupport.errorSummary(record.getError()));
        }
        return builder.toString();
    }

    /** 单行格式：慢日志 / 旧行为兼容 */
    private String buildSingleLineMessage(InvocationLogRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(text(record.getTarget())).append(']');
        append(builder, "cost", record.getCostMs() + "ms");
        if (record.isSlow()) {
            append(builder, "threshold", record.getSlowThreshold() + "ms");
        }
        append(builder, "args", text(record.getArgs()));
        if (record.isSuccess()) {
            append(builder, "result", text(record.getResult()));
        } else {
            appendQuoted(builder, "error", InvocationLogSupport.errorSummary(record.getError()));
        }
        return builder.toString();
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

    private Logger resolveLogger(String loggerName) {
        if (!StringUtils.hasText(loggerName)) {
            return FALLBACK_LOGGER;
        }
        return loggerCache.computeIfAbsent(loggerName, LoggerFactory::getLogger);
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
            @Override boolean isEnabled(Logger logger) { return logger.isTraceEnabled(); }
            @Override void log(Logger logger, String msg) { logger.trace(msg); }
            @Override void logError(Logger logger, String msg, Throwable t) { logger.trace(msg, t); }
        },
        DEBUG {
            @Override boolean isEnabled(Logger logger) { return logger.isDebugEnabled(); }
            @Override void log(Logger logger, String msg) { logger.debug(msg); }
            @Override void logError(Logger logger, String msg, Throwable t) { logger.debug(msg, t); }
        },
        INFO {
            @Override boolean isEnabled(Logger logger) { return logger.isInfoEnabled(); }
            @Override void log(Logger logger, String msg) { logger.info(msg); }
            @Override void logError(Logger logger, String msg, Throwable t) { logger.info(msg, t); }
        },
        WARN {
            @Override boolean isEnabled(Logger logger) { return logger.isWarnEnabled(); }
            @Override void log(Logger logger, String msg) { logger.warn(msg); }
            @Override void logError(Logger logger, String msg, Throwable t) { logger.warn(msg, t); }
        },
        ERROR {
            @Override boolean isEnabled(Logger logger) { return logger.isErrorEnabled(); }
            @Override void log(Logger logger, String msg) { logger.error(msg); }
            @Override void logError(Logger logger, String msg, Throwable t) { logger.error(msg, t); }
        };

        abstract boolean isEnabled(Logger logger);
        abstract void log(Logger logger, String msg);
        abstract void logError(Logger logger, String msg, Throwable t);
    }
}
