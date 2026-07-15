package io.github.luminion.velo.log.support;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationPhase;
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
        record.setLoggerName("com.example.UserController");
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget("127.0.0.1 GET /users/{id}");
        record.setCostMs(12);
        record.setSuccess(true);
        record.setArgs("{\"id\":1}");
        record.setResult("{\"name\":\"Tom\"}");

        writer.write(record);

        assertThat(output.getOut())
                .contains("com.example.UserController")
                .doesNotContain("traceId=")
                .doesNotContain("source=")
                .contains("[127.0.0.1 GET /users/{id}]")
                .contains("cost=12ms")
                .doesNotContain("status=")
                .contains("args={\"id\":1}")
                .contains("result={\"name\":\"Tom\"}");
    }

    @Test
    void shouldWriteUnifiedErrorLogWithoutStackTraceByDefault(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setTraceId("trace-001");
        record.setLoggerName("com.example.DemoClient");
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget("demo-client GET /fail");
        record.setCostMs(8);
        record.setSuccess(false);
        record.setArgs("{}");
        record.setError(new IllegalStateException("boom"));

        writer.write(record);

        assertThat(output.getOut())
                .contains("com.example.DemoClient")
                .contains("error=\"IllegalStateException: boom\"")
                .doesNotContain("at io.github");
    }

    @Test
    void shouldFallbackToOwnLoggerWhenLoggerNameIsNull(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget("127.0.0.1 GET /test");
        record.setCostMs(1);
        record.setSuccess(true);
        record.setArgs("-");
        record.setResult("-");

        writer.write(record);

        assertThat(output.getOut())
                .contains("Slf4JInvocationLogWriter")
                .doesNotContain("source=")
                .contains("[127.0.0.1 GET /test]");
    }

    @Test
    void shouldWriteEntryLineWithArrowAndArgs(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName("com.example.UserController");
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget("127.0.0.1 GET /users/{id}");
        record.setPhase(InvocationPhase.ENTRY);
        record.setSuccess(true);
        record.setArgs("{\"id\":1}");

        writer.write(record);

        assertThat(output.getOut())
                .contains("[127.0.0.1 GET /users/{id}] ==>")
                .contains("args={\"id\":1}")
                .doesNotContain("cost=")
                .doesNotContain("result=")
                .doesNotContain("<==");
    }

    @Test
    void shouldWriteExitLineWithBackArrowCostAndResult(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName("com.example.UserController");
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget("127.0.0.1 GET /users/{id}");
        record.setPhase(InvocationPhase.EXIT);
        record.setCostMs(12);
        record.setSuccess(true);
        record.setResult("{\"name\":\"Tom\"}");

        writer.write(record);

        assertThat(output.getOut())
                .contains("[127.0.0.1 GET /users/{id}] <==")
                .contains("cost=12ms")
                .contains("result={\"name\":\"Tom\"}")
                .doesNotContain("args=")
                .doesNotContain("==>");
    }

    @Test
    void shouldWriteExitLineWithErrorOnException(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName("com.example.DemoClient");
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget("GET /users/fail");
        record.setPhase(InvocationPhase.EXIT);
        record.setCostMs(8);
        record.setSuccess(false);
        record.setError(new IllegalStateException("boom"));

        writer.write(record);

        assertThat(output.getOut())
                .contains("[GET /users/fail] <==")
                .contains("cost=8ms")
                .contains("error=\"IllegalStateException: boom\"")
                .doesNotContain("args=");
    }

    @Test
    void shouldWriteSlowLogAtWarnLevelByDefault(CapturedOutput output) {
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(new VeloProperties());
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName("com.example.OrderService");
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget("placeOrder()");
        record.setCostMs(800);
        record.setSuccess(true);
        record.setSlow(true);
        record.setSlowThreshold(300);
        record.setArgs("{\"item\":\"book\"}");
        record.setResult("{\"ok\":true}");

        writer.write(record);

        // 慢日志以单行格式输出，且在 WARN 级别（日志行包含 WARN）
        assertThat(output.getOut())
                .contains("[placeOrder()]")
                .contains("cost=800ms")
                .contains("threshold=300ms")
                .doesNotContain("slow=true")
                .contains("args=")
                .contains("WARN");
    }

    @Test
    void shouldWriteSlowErrorAtErrorLevelWithConfiguredStackTrace(CapturedOutput output) {
        VeloProperties properties = new VeloProperties();
        properties.getLog().setLevel(LogLevel.OFF);
        properties.getLog().getInvocation().setIncludeErrorStackTrace(true);
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(properties);
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName("com.example.OrderService");
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget("placeOrder()");
        record.setCostMs(800);
        record.setSuccess(false);
        record.setSlow(true);
        record.setSlowThreshold(300);
        record.setArgs("{\"item\":\"book\"}");
        record.setError(new IllegalStateException("boom"));

        writer.write(record);

        assertThat(output.getOut())
                .contains("ERROR")
                .contains("threshold=300ms")
                .doesNotContain("slow=true")
                .contains("error=\"IllegalStateException: boom\"")
                .contains("at io.github.luminion.velo.log.support.Slf4JInvocationLogWriterTest");
    }

    @Test
    void shouldSkipSlowErrorWhenSlowLevelIsOff(CapturedOutput output) {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getSlow().setLevel(LogLevel.OFF);
        Slf4JInvocationLogWriter writer = new Slf4JInvocationLogWriter(properties);
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName("com.example.OrderService");
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget("cancelOrder()");
        record.setCostMs(800);
        record.setSuccess(false);
        record.setSlow(true);
        record.setSlowThreshold(300);
        record.setArgs("{\"item\":\"book\"}");
        record.setError(new IllegalStateException("boom"));

        writer.write(record);

        assertThat(output.getOut()).doesNotContain("[cancelOrder()]");
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

        assertThat(output.getOut()).doesNotContain("[demo-client GET /fail]");
    }
}
