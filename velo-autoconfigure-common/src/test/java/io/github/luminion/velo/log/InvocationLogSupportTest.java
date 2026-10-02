package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InvocationLogSupportTest {
  @Test
  void nullAndZeroLimitHaveExplicitSemantics() {
    assertThat(InvocationLogSupport.format(null, String::valueOf, -1)).isEqualTo("null");
    assertThat(
            InvocationLogSupport.format(
                new Object(),
                value -> {
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
                input -> {
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
    String text = InvocationLogSupport.format(values, String::valueOf, -1);
    assertThat(text).contains("safe=value", "stream=[omitted]", "cycle=[circular]");
  }

  @Test
  void normalizesNewlinesAndLimitsCustomFormatterOutput() {
    assertThat(InvocationLogSupport.format("anything", value -> "first\nsecond", -1))
        .isEqualTo("first\\nsecond");
    assertThat(InvocationLogSupport.format("anything", value -> "0123456789", 6))
        .isEqualTo("012...");
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
