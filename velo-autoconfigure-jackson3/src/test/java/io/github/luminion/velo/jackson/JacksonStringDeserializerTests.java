package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.github.luminion.velo.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.velo.xss.XssIgnore;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证 Jackson 3 字符串反序列化器保留原生强制转换与空值行为。 */
class JacksonStringDeserializerTests {

    /** 构造启用可选数字强制转换限制的测试 Mapper。 */
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

    /** 用于验证 {@code AS_EMPTY} 行为的字符串负载。 */
    static class EmptyPayload {

        /** 按 Jackson 空值规则转换为空字符串的属性。 */
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public String name;

        /** 同时忽略 XSS 清洗的空值属性。 */
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        @XssIgnore
        public String ignored;
    }

    /** 用于验证普通字符串属性强制转换的负载。 */
    static class Payload {

        /** 带属性级转换的字符串。 */
        public String name;

        /** 未标注转换注解的普通字符串。 */
        public String role;

        /** 忽略 XSS 清洗的字符串。 */
        @XssIgnore
        public String ignored;
    }
}
