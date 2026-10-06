package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InvocationLogSupportTest {
  @Test
  void formatsNullAndFullCustomText() {
    assertThat(InvocationLogSupport.format(null, String::valueOf)).isEqualTo("null");
    assertThat(InvocationLogSupport.format("anything", value -> "0123456789"))
        .isEqualTo("0123456789");
  }

  @Test
  void serializationFailureDoesNotUseToStringFallback() {
    Object value = new Object() {
      @Override
      public String toString() {
        throw new AssertionError();
      }
    };
    assertThat(InvocationLogSupport.format(value, input -> {
      throw new IllegalArgumentException();
    })).isEqualTo("serialization-failed");
  }

  @Test
  void sanitizesOnlyTechnicalValuesAndCircularContainers() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("safe", "value");
    values.put("stream", new InputStream() {
      @Override
      public int read() {
        throw new AssertionError();
      }
    });
    values.put("cycle", values);
    String text = InvocationLogSupport.format(values, String::valueOf);
    assertThat(text).contains("safe=value", "stream=[omitted]", "cycle=[circular]");
  }

  @Test
  void escapesControlsWithoutTruncatingTextOrUnicode() {
    String value = "a\ud83d\ude03b\nc\r\td\u2028\u0001more";
    assertThat(InvocationLogSupport.format(value, String::valueOf))
        .isEqualTo("a\ud83d\ude03b\\nc\\r\\td\\u2028\\u0001more");
  }

  @Test
  void retainsAllValuesAcrossNestedCollections() {
    List<Integer> values = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      values.add(i);
    }
    List<List<Integer>> nested = Collections.singletonList(values);
    assertThat(InvocationLogSupport.format(nested, String::valueOf))
        .isEqualTo(nested.toString());
  }

  @Test
  void retainsFullLargeString() {
    String source = String.join("", Collections.nCopies(10000, "x"));
    assertThat(InvocationLogSupport.format(source, String::valueOf)).isEqualTo(source);
  }

  @Test
  void comparesMillisecondThresholdWithNanosecondClock() {
    assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(TimeUnit.MICROSECONDS.toNanos(1500), 1))
        .isTrue();
    assertThat(InvocationLogSupport.exceedsSlowThresholdNanos(TimeUnit.MICROSECONDS.toNanos(500), 1))
        .isFalse();
  }
}
