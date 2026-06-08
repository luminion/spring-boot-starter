package io.github.luminion.velo.log.support;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class Slf4JInvocationLogWriterTest {

    @Test
    void shouldWriteUnifiedSuccessLog(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setTraceId("trace-001");
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget("127.0.0.1 GET /users/{id}");
        record.setCostMs(12);
        record.setSuccess(true);
        record.setArgs("{\"id\":1}");
        record.setResult("{\"name\":\"Tom\"}");

        writer.write(record);

        assertThat(output.getOut())
                .contains("traceId=trace-001")
                .contains("source=controller")
                .contains("target=\"127.0.0.1 GET /users/{id}\"")
                .contains("cost=12ms")
                .contains("status=success")
                .contains("args={\"id\":1}")
                .contains("result={\"name\":\"Tom\"}");
    }

    @Test
    void shouldWriteUnifiedErrorLogWithoutStackTraceByDefault(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setTraceId("trace-001");
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget("demo-client GET /fail");
        record.setCostMs(8);
        record.setSuccess(false);
        record.setArgs("{}");
        record.setError(new IllegalStateException("boom"));

        writer.write(record);

        assertThat(output.getOut())
                .contains("status=error")
                .contains("error=\"IllegalStateException: boom\"")
                .doesNotContain("at io.github");
    }

    @Test
    void shouldSkipErrorLogWhenLevelIsOff(CapturedOutput output) {
        VeloProperties properties = new VeloProperties();
        properties.getLog().setLevel(LogLevel.OFF);
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(properties);
        InvocationLogRecord record = new InvocationLogRecord();
        record.setTraceId("trace-001");
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget("demo-client GET /fail");
        record.setCostMs(8);
        record.setSuccess(false);
        record.setArgs("{}");
        record.setError(new IllegalStateException("boom"));

        writer.write(record);

        assertThat(output.getOut()).doesNotContain("traceId=trace-001");
    }
}
