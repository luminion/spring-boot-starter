package io.github.luminion.velo.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.log.core.InvocationLogFeature;
import io.github.luminion.velo.log.core.InvocationLogRecord;
import io.github.luminion.velo.log.core.InvocationLogSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
/** 验证日志输出器的功能记录格式、异常摘要和来源标识。 */
class Slf4JInvocationLogWriterTest {
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
    void offPreventsOutput(CapturedOutput output) {
        InvocationLogRecord record = record(InvocationLogFeature.ENTRY_ARGS);
        record.setLevel(LogLevel.OFF);
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
