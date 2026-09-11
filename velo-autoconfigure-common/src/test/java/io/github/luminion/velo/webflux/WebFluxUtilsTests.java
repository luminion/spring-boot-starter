package io.github.luminion.velo.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class WebFluxUtilsTests {

    @Test
    void shouldReadRequestPropertiesAndProxyIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "https://api.example.test/users/1?token=secret")
                        .header("X-Real-IP", "203.0.113.7")
                        .remoteAddress(new InetSocketAddress("10.0.0.9", 8080))
                        .build());

        assertThat(WebFluxUtils.getRequestURI(exchange)).isEqualTo("/users/1");
        assertThat(WebFluxUtils.getRequestUrl(exchange)).isEqualTo("https://api.example.test/users/1");
        assertThat(WebFluxUtils.getRequestMethod(exchange)).isEqualTo("GET");
        assertThat(WebFluxUtils.getRequestQueryString(exchange)).isEqualTo("token=secret");
        assertThat(WebFluxUtils.getRequestParameter(exchange, "token")).isEqualTo("secret");
        assertThat(WebFluxUtils.getRequestIp(exchange)).isEqualTo("203.0.113.7");
        assertThat(WebFluxUtils.getRequestServerName(exchange)).isEqualTo("api.example.test");
        assertThat(WebFluxUtils.getRequestServerPort(exchange)).isEqualTo(-1);
    }

    @Test
    void shouldPreferBestMatchingPathAndWriteJsonResponseReactively() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/users/1").build());
        exchange.getAttributes().put(org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/users/{id}");

        assertThat(WebFluxUtils.getRequestPath(exchange)).isEqualTo("/users/{id}");

        WebFluxUtils.writeResponseJson(exchange, "{\"ok\":true}").block();

        assertThat(exchange.getResponse().getHeaders().getContentType().toString())
                .contains("application/json")
                .contains("UTF-8");
        assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo("{\"ok\":true}");
    }
}
