package io.github.luminion.velo.webflux;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebFluxRuntimeJsonSerializerTests {

    @Test
    void shouldUseConfiguredWebFluxJsonEncoder() {
        RuntimeJsonSerializer serializer = new WebFluxRuntimeJsonSerializer(Collections.singletonList(
                new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder(new ObjectMapper()))));

        assertThat(serializer.toJson(new DemoPayload("tomUser"))).contains("\"userName\":\"tomUser\"");
    }

    @Test
    void shouldOmitUnsafeWebFluxValuesAndKeepJsonStringSemantics() {
        RuntimeJsonSerializer serializer = new WebFluxRuntimeJsonSerializer(Collections.singletonList(
                new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder(new ObjectMapper()))));
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", 1L);
        values.put("exchange", MockServerWebExchange.from(MockServerHttpRequest.get("/").build()));

        assertThat(serializer.toJson(values))
                .contains("\"id\":1")
                .contains("\"exchange\":\"[omitted]\"");
        assertThat(serializer.toJson("hello")).isEqualTo("\"hello\"");
    }

    static class DemoPayload {

        private final String userName;

        DemoPayload(String userName) {
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }
    }
}
