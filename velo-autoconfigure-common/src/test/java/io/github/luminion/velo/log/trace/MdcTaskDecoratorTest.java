package io.github.luminion.velo.log.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {
  @AfterEach
  void clear() {
    MDC.clear();
  }

  @Test
  void capturesSubmitContextAsSnapshotAndRestoresWorker() throws InterruptedException {
    MDC.put("traceId", "submitted");
    MDC.put("user", "alice");
    List<String> seen = new ArrayList<>();
    Runnable task =
        new MdcTaskDecorator("traceId", new HeaderTraceContextResolver("X-Trace-Id"))
            .decorate(
                () -> {
                  seen.add(MDC.get("traceId"));
                  seen.add(MDC.get("user"));
                });
    MDC.put("traceId", "changed-later");
    AtomicReference<String> after = new AtomicReference<>();
    Thread worker =
        new Thread(
            () -> {
              MDC.put("traceId", "worker");
              task.run();
              after.set(MDC.get("traceId"));
              MDC.clear();
            });
    worker.start();
    worker.join();
    assertThat(seen).containsExactly("submitted", "alice");
    assertThat(after).hasValue("worker");
    assertThat(MDC.get("traceId")).isEqualTo("changed-later");
  }

  @Test
  void reusedWorkerGetsNewTraceForEveryRootTask() {
    List<String> ids = new ArrayList<>();
    MdcTaskDecorator decorator = new MdcTaskDecorator();
    Runnable first = decorator.decorate(() -> ids.add(MDC.get("traceId")));
    Runnable second = decorator.decorate(() -> ids.add(MDC.get("traceId")));
    MDC.put("traceId", "worker-leftover");
    MDC.put("user", "worker-user");
    first.run();
    second.run();
    assertThat(ids).allMatch(id -> id.matches("[0-9a-f]{32}"));
    assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    assertThat(MDC.get("traceId")).isEqualTo("worker-leftover");
    assertThat(MDC.get("user")).isEqualTo("worker-user");
  }

  @Test
  void failureRestoresWorkerAndDoesNotSwallowException() {
    IllegalStateException error = new IllegalStateException("failure");
    Runnable task =
        new MdcTaskDecorator()
            .decorate(
                () -> {
                  MDC.put("user", "task-user");
                  throw error;
                });
    MDC.put("traceId", "worker");
    assertThatThrownBy(task::run).isSameAs(error);
    assertThat(MDC.get("traceId")).isEqualTo("worker");
    assertThat(MDC.get("user")).isNull();
  }

  @Test
  void customKeyGeneratesTaskTraceWithoutChangingOtherKeys() {
    MDC.put("traceId", "unrelated");
    AtomicReference<String> seen = new AtomicReference<>();
    Runnable task =
        new MdcTaskDecorator("requestId").decorate(() -> seen.set(MDC.get("requestId")));
    task.run();
    assertThat(seen.get()).matches("[0-9a-f]{32}");
    assertThat(MDC.get("requestId")).isNull();
    assertThat(MDC.get("traceId")).isEqualTo("unrelated");
  }

  @Test
  void completeW3cSnapshotIsCapturedAndWorkerSnapshotIsRestoredEvenOnFailure() {
    TraceData submitted = new W3cTraceContextResolver().resolve();
    TraceData worker = new W3cTraceContextResolver().resolve();
    AtomicReference<TraceData> seen = new AtomicReference<>();
    IllegalStateException failure = new IllegalStateException("failure");
    Runnable task;
    try (TraceContext.Scope scope = TraceContext.install("traceId", submitted, true)) {
      task =
          new MdcTaskDecorator()
              .decorate(
                  () -> {
                    seen.set(TraceContext.current());
                    assertThat(MDC.get("traceId")).isEqualTo(submitted.getTraceId());
                    throw failure;
                  });
    }
    try (TraceContext.Scope scope = TraceContext.install("traceId", worker, true)) {
      assertThatThrownBy(task::run).isSameAs(failure);
      assertThat(TraceContext.current()).isSameAs(worker);
      assertThat(MDC.get("traceId")).isEqualTo(worker.getTraceId());
    }
    assertThat(seen).hasValue(submitted);
    assertThat(TraceContext.current()).isNull();
    assertThat(MDC.get("traceId")).isNull();
  }

  @Test
  void absentSubmitContextUsesCustomResolverForEachIndependentTask() {
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    TraceContextResolver resolver = () -> new TraceData("custom-" + calls.incrementAndGet(), null);
    MdcTaskDecorator decorator = new MdcTaskDecorator("traceId", resolver);
    List<String> seen = new ArrayList<>();
    Runnable first = decorator.decorate(() -> seen.add(MDC.get("traceId")));
    Runnable second = decorator.decorate(() -> seen.add(MDC.get("traceId")));
    first.run();
    second.run();
    assertThat(seen).containsExactly("custom-1", "custom-2");
    assertThat(TraceContext.current()).isNull();
  }
}
