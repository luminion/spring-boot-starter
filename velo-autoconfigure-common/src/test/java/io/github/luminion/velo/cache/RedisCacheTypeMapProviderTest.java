package io.github.luminion.velo.cache;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisCacheTypeMapProviderTest {
    @Test
    void shouldAcceptClassesAndCompleteGenericTypesWithoutExposingTheInternalMap() {
        Type listType = new ParameterizedTypeReference<List<String>>() { }.getType();
        Map<String, Type> types = new LinkedHashMap<>();
        types.put("single", String.class);
        types.put("list", listType);
        RedisCacheTypeMapProvider provider = new RedisCacheTypeMapProvider(types);
        types.clear();
        assertThat(provider.getCacheTypes()).containsEntry("single", String.class).containsEntry("list", listType);
        provider.getCacheTypes().clear();
        assertThat(provider.getCacheTypes()).hasSize(2);
    }

    @Test
    void shouldRejectMissingNamesTypesAndRawGenericContainers() {
        assertThatThrownBy(() -> new RedisCacheTypeMapProvider(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisCacheTypeMapProvider(Collections.singletonMap(" ", String.class)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisCacheTypeMapProvider(Collections.singletonMap("single", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisCacheTypeMapProvider(Collections.singletonMap("list", List.class)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("完整泛型");
    }
}
