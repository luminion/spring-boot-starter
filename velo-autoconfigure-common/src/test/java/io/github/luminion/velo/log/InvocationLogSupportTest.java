package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InvocationLogSupportTest {
  @Test
  void nullAndZeroLimitHaveExplicitSemantics() {
    assertThat(InvocationLogSupport.format(null, (logValue, logOutput) -> logOutput.write(String.valueOf(logValue)), -1)).isEqualTo("null");
    assertThat(
            InvocationLogSupport.format(
                new Object(),
                (value, output) -> {
                  throw new AssertionError();
                },
                0))
        .isEqualTo("disabled");
  }

  @Test
  void serializationFailureDoesNotUseToStringFallback() {
    Object value =
        new Object() {
          public String toString() {
            throw new AssertionError();
          }
        };
    assertThat(
            InvocationLogSupport.format(
                value,
                (input, output) -> {
                  throw new IllegalArgumentException();
                },
                -1))
        .isEqualTo("serialization-failed");
  }

  @Test
  void sanitizesOnlyTechnicalValuesAndCircularContainers() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("safe", "value");
    values.put(
        "stream",
        new InputStream() {
          public int read() {
            throw new AssertionError();
          }
        });
    values.put("cycle", values);
    String text = InvocationLogSupport.format(values, (logValue, logOutput) -> logOutput.write(String.valueOf(logValue)), -1);
    assertThat(text).contains("safe=value", "stream=[omitted]", "cycle=[circular]");
  }

  @Test
  void normalizesNewlinesAndLimitsCustomFormatterOutput() {
    assertThat(InvocationLogSupport.format("anything", (value, output) -> output.write("first\nsecond"), -1))
        .isEqualTo("first\\nsecond");
    assertThat(InvocationLogSupport.format("anything", (value, output) -> output.write("0123456789"), 6))
        .isEqualTo("012...");
  }

  @Test
  void stopsStreamingFormatterAndRecognizesWrappedLimitSignal() {
    AtomicInteger writes = new AtomicInteger();
    String text = InvocationLogSupport.format("input", (value, output) -> {
      try {
        for (int i = 0; i < 100000; i++) {
          writes.incrementAndGet();
          output.write('x');
        }
      } catch (IOException error) {
        throw new IllegalStateException("wrapped", error);
      }
    }, 6);
    assertThat(text).isEqualTo("xxx...");
    assertThat(writes).hasValue(7);
  }

  @Test
  void countsEscapingAndEllipsisWithoutSplittingSurrogatePairs() {
    String value = "a\ud83d\ude03b\nc\r\td\u2028\u0001more";
    for (int limit = 1; limit <= 24; limit++) {
      String text = InvocationLogSupport.format(value, (input, output) -> {
        for (int i = 0; i < value.length(); i++) {
          output.write(value.charAt(i));
        }
      }, limit);
      assertThat(text.length()).isLessThanOrEqualTo(limit);
      assertThat(text).doesNotContain("\n", "\r", "\t", "\u2028", "\u0001");
      for (int i = 0; i < text.length(); i++) {
        if (Character.isHighSurrogate(text.charAt(i))) {
          assertThat(i + 1).isLessThan(text.length());
          assertThat(Character.isLowSurrogate(text.charAt(++i))).isTrue();
        } else {
          assertThat(Character.isLowSurrogate(text.charAt(i))).isFalse();
        }
      }
    }
    assertThat(InvocationLogSupport.format("a\nb", (input, output) -> output.write("a\nb"), 4))
        .isEqualTo("a\\nb");
    assertThat(InvocationLogSupport.format("\nabcdef", (input, output) -> output.write("\nabcdef"), 4))
        .isEqualTo("...");
  }

  @Test
  void containerBudgetIsSharedAcrossNestedCollections() {
    AtomicInteger visits = new AtomicInteger();
    List<Object> values = new AbstractList<Object>() {
      @Override
      public Object get(int index) {
        visits.incrementAndGet();
        return new AbstractList<Integer>() {
          @Override
          public Integer get(int child) {
            visits.incrementAndGet();
            return child;
          }

          @Override
          public int size() {
            return 100000;
          }
        };
      }

      @Override
      public int size() {
        return 100000;
      }
    };
    String text = InvocationLogSupport.format(values,
        (input, output) -> output.write(String.valueOf(input)), -1);
    assertThat(visits.get()).isLessThanOrEqualTo(255);
    assertThat(text).contains("[omitted]").endsWith("...");
  }

  @Test
  void genuineFailureUsesMarkerAndUnlimitedStringStillWorks() {
    assertThat(InvocationLogSupport.format("input", (input, output) -> {
      output.write("partial");
      throw new IOException("actual failure");
    }, 64)).isEqualTo("serialization-failed");
    assertThat(InvocationLogSupport.format("input", (input, output) -> {
      throw new IOException("actual failure");
    }, 6)).isEqualTo("ser...");
    StringBuilder source = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      source.append('x');
    }
    assertThat(InvocationLogSupport.format(source.toString(),
        (input, output) -> output.write((String) input), -1)).isEqualTo(source.toString());
  }

  @Test
  void comparesMillisecondThresholdWithNanosecondClock() {
    assertThat(
            InvocationLogSupport.exceedsSlowThresholdNanos(TimeUnit.MICROSECONDS.toNanos(1500), 1))
        .isTrue();
    assertThat(
            InvocationLogSupport.exceedsSlowThresholdNanos(TimeUnit.MICROSECONDS.toNanos(500), 1))
        .isFalse();
  }
}
