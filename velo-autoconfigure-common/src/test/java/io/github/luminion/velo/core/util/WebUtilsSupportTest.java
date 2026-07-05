package io.github.luminion.velo.core.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebUtilsSupportTest {

    private static String resolve(Map<String, String> headers, String remoteAddr) {
        return WebUtilsSupport.resolveClientIp(headers::get, remoteAddr);
    }

    @Test
    void shouldConvertExpandedIpv6LoopbackToIpv4() {
        assertThat(resolve(new HashMap<>(), "0:0:0:0:0:0:0:1")).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldConvertCompressedIpv6LoopbackToIpv4() {
        assertThat(resolve(new HashMap<>(), "::1")).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldConvertCompressedIpv6LoopbackFromProxyHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Real-IP", "::1");
        assertThat(resolve(headers, "10.0.0.9")).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldTakeFirstValidIpFromForwardedChain() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-forwarded-for", "203.0.113.7, 10.0.0.1");
        assertThat(resolve(headers, "10.0.0.9")).isEqualTo("203.0.113.7");
    }

    @Test
    void shouldFallBackToRemoteAddrWhenAllHeadersUnknown() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-forwarded-for", "unknown, unknown");
        assertThat(resolve(headers, "192.168.1.5")).isEqualTo("192.168.1.5");
    }
}
