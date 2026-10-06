package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.luminion.velo.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.velo.xss.XssIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonStringDeserializerTests {
    private JsonMapper mapper(boolean rejectNumbers) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new JacksonStringDeserializer(type -> value -> value, value -> value));
        JsonMapper.Builder builder = JsonMapper.builder().addModule(module)
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        if (rejectNumbers) {
            builder.withCoercionConfig(LogicalType.Textual,
                    config -> config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail));
        }
        return builder.build();
    }

    @Test
    void shouldRejectStructuredStringValuesWithoutReadingNestedDtoFields() {
        JsonMapper mapper = mapper(false);
        for (String json : new String[]{"{\"name\":{\"role\":\"admin\"},\"role\":\"user\"}",
                "{\"name\":[\"admin\"],\"role\":\"user\"}",
                "{\"ignored\":{\"role\":\"admin\"},\"role\":\"user\"}"}) {
            assertThatThrownBy(() -> mapper.readValue(json, Payload.class)).isInstanceOf(Exception.class);
        }
    }

    @Test
    void shouldHonorNativeCoercionForDefaultAndContextualStringDeserializer() throws Exception {
        JsonMapper strict = mapper(true);
        assertThatThrownBy(() -> strict.readValue("{\"name\":42}", Payload.class)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> strict.readValue("{\"ignored\":42}", Payload.class)).isInstanceOf(Exception.class);
        Payload converted = mapper(false).readValue("{\"name\":42,\"role\":\"user\"}", Payload.class);
        assertThat(converted.name).isEqualTo("42");
        assertThat(converted.role).isEqualTo("user");
    }

    @Test
    void shouldPreserveJacksonNullAsEmptyForContextualStrings() throws Exception {
        EmptyPayload payload = mapper(false).readValue("{\"name\":null,\"ignored\":null}", EmptyPayload.class);
        assertThat(payload.name).isEmpty();
        assertThat(payload.ignored).isEmpty();
    }

    static class EmptyPayload {
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public String name;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        @XssIgnore
        public String ignored;
    }

    static class Payload {
        public String name;
        public String role;
        @XssIgnore
        public String ignored;
    }
}
