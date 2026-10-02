package io.github.luminion.velo.log.support;

import io.github.luminion.velo.log.InvocationLogFeature;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.StringUtils;

/**
 * 单功能 SLF4J 输出器；不判定业务异常，不输出堆栈或重复关联字段。
 */
public class Slf4JInvocationLogWriter implements InvocationLogWriter {
    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();

    @Override
    public boolean isEnabled(InvocationLogRecord record) {
        if (record == null || record.getFeature() == null || record.getLevel() == LogLevel.OFF) {
            return false;
        }
        Logger logger = logger(record);
        switch (level(record)) {
            case TRACE:
                return logger.isTraceEnabled();
            case DEBUG:
                return logger.isDebugEnabled();
            case WARN:
                return logger.isWarnEnabled();
            case ERROR:
            case FATAL:
                return logger.isErrorEnabled();
            default:
                return logger.isInfoEnabled();
        }
    }

    @Override
    public void write(InvocationLogRecord record) {
        if (!isEnabled(record)) {
            return;
        }
        String message = message(record);
        Logger logger = logger(record);
        switch (level(record)) {
            case TRACE:
                logger.trace(message);
                break;
            case DEBUG:
                logger.debug(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
            case FATAL:
                logger.error(message);
                break;
            default:
                logger.info(message);
        }
    }

    private LogLevel level(InvocationLogRecord record) {
        if (record.getLevel() != null) {
            return record.getLevel();
        }
        return record.getFeature() == InvocationLogFeature.SLOW_LOG
                || record.getFeature() == InvocationLogFeature.ERROR_LOG
                ? LogLevel.WARN
                : LogLevel.INFO;
    }

    private Logger logger(InvocationLogRecord record) {
        String name =
                StringUtils.hasText(record.getLoggerName()) ? record.getLoggerName() : getClass().getName();
        return loggers.computeIfAbsent(name, LoggerFactory::getLogger);
    }

    private String message(InvocationLogRecord record) {
        String source = record.getSource() == null ? "unknown" : record.getSource().getValue();
        String prefix =
                "["
                        + source
                        + "] ["
                        + InvocationLogSupport.singleLine(record.getTarget())
                        + "] "
                        + (record.getFeature().getPhase() == InvocationPhase.ENTRY ? "==> " : "<== ")
                        + record.getFeature().getLabel()
                        + "=";
        return prefix + InvocationLogSupport.singleLine(record.getContent());
    }
}
