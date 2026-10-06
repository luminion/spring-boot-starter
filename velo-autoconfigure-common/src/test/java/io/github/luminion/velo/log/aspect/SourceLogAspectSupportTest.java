package io.github.luminion.velo.log.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogFeature;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.TraceData;
import io.github.luminion.velo.log.trace.TraceScopeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SourceLogAspectSupportTest {
  @AfterEach
  void clear() {
    MDC.clear();
  }

  @Test
  void eachScheduledExecutionCreatesRootAndRestoresCaller() throws Throwable {
    VeloProperties p = new VeloProperties();
    List<InvocationLogRecord> records = new ArrayList<>();
    List<String> traces = new ArrayList<>();
    p.getLog().getSources().getScheduled().getSlowLog().setThresholdMs(0L);
    ScheduledLogAspect aspect =
        new ScheduledLogAspect(
            p,
            new InvocationLogEngine(
                p,
                String::valueOf,
                record -> {
                  records.add(record);
                  traces.add(MDC.get("traceId"));
                }));
    MDC.put("traceId", "caller");
    MDC.put("user", "alice");
    aspect.around(point());
    aspect.around(point());
    assertThat(records)
        .hasSize(2)
        .allSatisfy(
            r -> {
              assertThat(r.getFeature()).isEqualTo(InvocationLogFeature.SLOW_LOG);
              assertThat(r.getSource()).isEqualTo(InvocationLogSource.SCHEDULED);
            });
    assertThat(traces).hasSize(2).allMatch(id -> id.matches("[0-9a-f]{32}"));
    assertThat(traces.get(0)).isNotEqualTo(traces.get(1));
    assertThat(MDC.get("traceId")).isEqualTo("caller");
    assertThat(MDC.get("user")).isEqualTo("alice");
  }

  @Test
  void xxlFailureCreatesIndependentRootAndPreservesOriginalException() throws Throwable {
    VeloProperties p = new VeloProperties();
    List<InvocationLogRecord> records = new ArrayList<>();
    List<String> traces = new ArrayList<>();
    XxlJobLogAspect aspect =
        new XxlJobLogAspect(
            p,
            new InvocationLogEngine(
                p,
                String::valueOf,
                record -> {
                  records.add(record);
                  traces.add(MDC.get("traceId"));
                }));
    ProceedingJoinPoint point = point();
    IllegalStateException error = new IllegalStateException("task-failed");
    when(point.proceed()).thenThrow(error);
    MDC.put("traceId", "caller");
    assertThatThrownBy(() -> aspect.around(point)).isSameAs(error);
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getFeature()).isEqualTo(InvocationLogFeature.ERROR_LOG);
    assertThat(traces.get(0)).matches("[0-9a-f]{32}").isNotEqualTo("caller");
    assertThat(MDC.get("traceId")).isEqualTo("caller");
  }

  @Test
  void scheduledAndXxlJobUseConfiguredResolverForEachExecution() throws Throwable {
    VeloProperties properties = new VeloProperties();
    AtomicInteger calls = new AtomicInteger();
    TraceContextResolver resolver =
        () -> new TraceData("task-" + calls.incrementAndGet(), Collections.emptyMap());
    InvocationLogEngine engine = new InvocationLogEngine(properties, String::valueOf, r -> {});
    TraceScopeManager trace = new TraceScopeManager(properties.getLog().getTrace(), resolver);
    ProceedingJoinPoint point = point();
    List<String> ids = new ArrayList<>();
    when(point.proceed())
        .thenAnswer(
            c -> {
              ids.add(TraceContext.current().getTraceId());
              return null;
            });
    MDC.put("traceId", "caller");
    new ScheduledLogAspect(engine, trace).around(point);
    new XxlJobLogAspect(engine, trace).around(point);
    assertThat(ids).containsExactly("task-1", "task-2");
    assertThat(calls).hasValue(2);
    assertThat(MDC.get("traceId")).isEqualTo("caller");
    assertThat(TraceContext.current()).isNull();
  }

  @Test
  void disabledTraceLeavesMdcUntouched() throws Throwable {
    VeloProperties p = new VeloProperties();
    p.getLog().getTrace().setEnabled(false);
    ScheduledLogAspect aspect =
        new ScheduledLogAspect(p, new InvocationLogEngine(p, String::valueOf, r -> {}));
    ProceedingJoinPoint point = point();
    MDC.put("traceId", "caller");
    when(point.proceed())
        .thenAnswer(
            c -> {
              assertThat(MDC.get("traceId")).isEqualTo("caller");
              return null;
            });
    aspect.around(point());
    assertThat(MDC.get("traceId")).isEqualTo("caller");
  }

  private ProceedingJoinPoint point() throws Exception {
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(Task.class.getMethod("execute", String.class));
    when(signature.getDeclaringType()).thenReturn(Task.class);
    when(signature.getDeclaringTypeName()).thenReturn(Task.class.getName());
    when(signature.getName()).thenReturn("execute");
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    when(point.getSignature()).thenReturn(signature);
    when(point.getTarget()).thenReturn(new Task());
    when(point.getArgs()).thenReturn(new Object[] {"payload"});
    return point;
  }

  static class Task {
    public void execute(String payload) {}
  }
}
