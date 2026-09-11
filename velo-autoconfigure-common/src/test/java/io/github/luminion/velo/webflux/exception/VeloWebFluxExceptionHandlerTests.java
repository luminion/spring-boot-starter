package io.github.luminion.velo.webflux.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.web.server.ServerWebInputException;

import static org.assertj.core.api.Assertions.assertThat;

class VeloWebFluxExceptionHandlerTests {

    @Test
    void shouldMapReactiveInputErrorsToFailedResponse() {
        VeloWebFluxExceptionHandler<String> handler = new VeloWebFluxExceptionHandler<>(
                message -> "failed:" + message, error -> "error");

        assertThat(handler.handleServerWebInputException(new ServerWebInputException("参数错误")))
                .isEqualTo("failed:参数错误");
        assertThat(handler.handleDataBufferLimitException(new DataBufferLimitException("too large")))
                .isEqualTo("failed:请求数据超过大小限制");
    }

    @Test
    void shouldPreserveBusinessExceptionFallback() {
        VeloWebFluxExceptionHandler<String> handler = new VeloWebFluxExceptionHandler<>(
                message -> "failed:" + message, error -> "error", IllegalArgumentException.class);

        assertThat(handler.handleGlobalException(new IllegalArgumentException("business")))
                .isEqualTo("failed:business");
        assertThat(handler.handleGlobalException(new IllegalStateException("system")))
                .isEqualTo("error");
    }
}
