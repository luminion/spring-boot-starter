package io.github.luminion.velo.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvocationUtilsTest {

    @Test
    void shouldRejectSelfReferentialCollectionWithoutStackOverflow() {
        List<Object> value = new ArrayList<>();
        value.add(value);

        assertThat(InvocationUtils.isLoggableValue(value)).isFalse();
        assertThat(InvocationUtils.formatValue(value, 0)).isEqualTo("[omitted]");
    }

    @Test
    void shouldRejectSelfReferentialMapWithoutStackOverflow() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("self", value);

        assertThat(InvocationUtils.isLoggableValue(value)).isFalse();
    }

    @Test
    void shouldRejectSelfReferentialArrayWithoutStackOverflow() {
        Object[] value = new Object[1];
        value[0] = value;

        assertThat(InvocationUtils.isLoggableValue(value)).isFalse();
    }

    @Test
    void shouldRejectExcessivelyNestedContainers() {
        Object value = "leaf";
        for (int i = 0; i < 64; i++) {
            List<Object> wrapper = new ArrayList<>();
            wrapper.add(value);
            value = wrapper;
        }

        assertThat(InvocationUtils.isLoggableValue(value)).isFalse();
    }

    @Test
    void shouldAllowRepeatedReferenceWhenItIsNotCircular() {
        List<String> shared = new ArrayList<>();
        shared.add("value");
        List<Object> value = new ArrayList<>();
        value.add(shared);
        value.add(shared);

        assertThat(InvocationUtils.isLoggableValue(value)).isTrue();
    }
}
