package io.github.luminion.velo.log.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogFeature;
import io.github.luminion.velo.log.core.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.LogValueFormatter;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.LogIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class InvokeLogAspectTest {

  @Test
  void shouldCaptureFinishArgsForNormalAndExceptionalCompletion() {
    LogValueFormatter formatter =
        value -> {
          if (!(value instanceof Map)) {
            return String.valueOf(value);
          }
          Object argument = ((Map<?, ?>) value).values().iterator().next();
          String text = argument instanceof MutableArgument
              ? ((MutableArgument) argument).value
              : String.valueOf(value);
          return text;
        };
    CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
    InvokeLogAspect aspect =
        new InvokeLogAspect(new InvocationLogEngine(new VeloProperties(), formatter, writer));
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DemoService());
    proxyFactory.setInterfaces(DemoOperations.class);
    proxyFactory.addAspect(aspect);
    DemoOperations proxy = proxyFactory.getProxy();

    MutableArgument successfulArgument = new MutableArgument("before");
    proxy.complete(successfulArgument);

    assertThat(writer.records)
        .extracting(InvocationLogRecord::getFeature)
        .containsExactly(
            InvocationLogFeature.ENTRY_ARGS,
            InvocationLogFeature.EXIT_ARGS,
            InvocationLogFeature.EXIT_RESULT);
    assertThat(writer.records.get(0).getContent()).isEqualTo("before");
    assertThat(writer.records.get(1).getContent()).isEqualTo("complete");
    assertThat(writer.records.get(2).getContent()).isEqualTo("void");

    MutableArgument failedArgument = new MutableArgument("before");
    assertThatThrownBy(() -> proxy.fail(failedArgument))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(writer.records).hasSize(6);
    assertThat(writer.records.get(4).getFeature()).isEqualTo(InvocationLogFeature.EXIT_ARGS);
    assertThat(writer.records.get(4).getContent()).isEqualTo("failed");
    assertThat(writer.records.get(5).getContent()).contains(IllegalStateException.class.getName());

    MutableArgument ignoredArgument = new MutableArgument("secret");
    assertThat(proxy.ignore(ignoredArgument)).isSameAs(ignoredArgument);

    assertThat(writer.records).hasSize(6);
  }

  interface DemoOperations {

    void complete(MutableArgument argument);

    void fail(MutableArgument argument);

    MutableArgument ignore(MutableArgument argument);
  }

  static class DemoService implements DemoOperations {

    @Override
    @InvokeLog
    @ExitArgs
    public void complete(MutableArgument argument) {
      argument.value = "complete";
    }

    @Override
    @InvokeLog
    @ExitArgs
    public void fail(MutableArgument argument) {
      argument.value = "failed";
      throw new IllegalStateException("boom");
    }

    @Override
    @InvokeLog
    @LogIgnore
    public MutableArgument ignore(MutableArgument argument) {
      return argument;
    }
  }

  static class MutableArgument {

    private String value;

    MutableArgument(String value) {
      this.value = value;
    }
  }

  static class CapturingInvocationLogWriter implements InvocationLogWriter {

    private final List<InvocationLogRecord> records = new ArrayList<>();

    @Override
    public void write(InvocationLogRecord record) {
      records.add(record);
    }
  }
}
