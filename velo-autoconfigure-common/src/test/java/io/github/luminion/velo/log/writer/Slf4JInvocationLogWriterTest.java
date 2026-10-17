package io.github.luminion.velo.log.writer;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
/**
 * 验证日志输出器的功能记录格式、异常摘要和来源标识。
 */
class Slf4JInvocationLogWriterTest {
    @ParameterizedTest
    @EnumSource(value = LogPolicyLevel.class, names = "OFF", mode = EnumSource.Mode.EXCLUDE)
    void writesEachExplicitLevelThroughSlf4j(LogPolicyLevel level, CapturedOutput output) {
        String loggerName = getClass().getName() + ".levels";
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        try {
            InvocationLogRecord record = record(InvocationLogFeature.COST_LOG);
            record.setLoggerName(loggerName);
            record.setLevel(level);
            record.setContent("explicit-" + level);

            new Slf4JInvocationLogWriter().write(record);

            assertThat(output.getOut()).contains(" " + level + " ", "cost=explicit-" + level);
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    @Test
    void writesOneLinePerFeatureWithoutRepeatedMetadata(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter();
        for (InvocationLogFeature feature : InvocationLogFeature.values()) {
            InvocationLogRecord record = record(feature);
            if (feature == InvocationLogFeature.COST_LOG) {
                record.setContent("5ms");
            } else if (feature == InvocationLogFeature.SLOW_LOG) {
                record.setContent("{cost=5ms, threshold=0ms}");
            } else if (feature == InvocationLogFeature.ERROR_LOG) {
                record.setContent("{\"type\":\"example.BusinessException\",\"message\":\"bad\"}");
            } else {
                record.setContent("{\"id\":1}");
            }
            writer.write(record);
        }
        assertThat(output.getOut())
                .contains(
                        "[annotation] [work()] ==> entryArgs={\"id\":1}",
                        "[annotation] [work()] <== exitArgs={\"id\":1}",
                        "[annotation] [work()] <== exitResult={\"id\":1}",
                        "[annotation] [work()] <== cost=5ms",
                        "[annotation] [work()] <== slow={cost=5ms, threshold=0ms}",
                        "[annotation] [work()] ==> requestHeaders=",
                        "[annotation] [work()] <== responseHeaders=",
                        "[annotation] [work()] <=="
                                + " error={\"type\":\"example.BusinessException\",\"message\":\"bad\"}")
                .doesNotContain("event=", "source=", "traceId=", "invocationId=", "success=");
    }

    @Test
    void errorIsWarnSummaryWithoutStackTraceAndCannotInjectNewlines(CapturedOutput output) {
        InvocationLogRecord record = record(InvocationLogFeature.ERROR_LOG);
        record.setContent(
                "{\"type\":\"example.BusinessException\",\"message\":\"bad\nmessage\\\"quoted\\\"\"}");
        new Slf4JInvocationLogWriter().write(record);
        assertThat(output.getOut())
                .contains(
                        "WARN",
                        "error={\"type\":\"example.BusinessException\",\"message\":\"bad\\n"
                                + "message\\\"quoted\\\"\"}")
                .doesNotContain("bad\nmessage", "\tat ");
    }

    @ParameterizedTest
    @EnumSource(InvocationLogSource.class)
    void identifiesEveryEntrySource(InvocationLogSource source, CapturedOutput output) {
        InvocationLogRecord record = record(InvocationLogFeature.ENTRY_ARGS);
        record.setSource(source);
        record.setContent("{}");

        new Slf4JInvocationLogWriter().write(record);

        assertThat(output.getOut()).contains("[" + source.getValue() + "] [work()] ==> entryArgs={}");
    }

    @Test
    void writesPublicBlockBetweenMethodAndFeatureAndEscapesControlCharacters(CapturedOutput output) {
        InvocationLogRecord record = record(InvocationLogFeature.COST_LOG);
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setContextContent("ip=127.0.0.1 route=/users\nforged");
        record.setContent("5ms");
        new Slf4JInvocationLogWriter().write(record);
        assertThat(output.getOut())
                .contains("[controller] [work()] [ip=127.0.0.1 route=/users\\nforged] <== cost=5ms")
                .doesNotContain("/users\nforged");
    }

    @Test
    void offPreventsOutput(CapturedOutput output) {
        InvocationLogRecord record = record(InvocationLogFeature.ENTRY_ARGS);
        record.setLevel(LogPolicyLevel.OFF);
        record.setContent("hidden-payload");
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter();
        assertThat(writer.isEnabled(record)).isFalse();
        writer.write(record);
        assertThat(output.getOut()).doesNotContain("hidden-payload");
    }

    private InvocationLogRecord record(InvocationLogFeature feature) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setFeature(feature);
        record.setSource(InvocationLogSource.ANNOTATION);
        record.setTarget("work()");
        return record;
    }
}
