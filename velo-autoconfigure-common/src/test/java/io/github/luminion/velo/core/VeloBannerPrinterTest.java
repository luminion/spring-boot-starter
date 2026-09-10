package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloBannerPrinterTest {

    @Test
    void shouldPreserveSubSecondAndFractionalSecondPrecision() throws Exception {
        assertThat(printBanner(Duration.ofMillis(500))).contains("ttl=500ms");
        assertThat(printBanner(Duration.ofMillis(5_500))).contains("ttl=5.5s");
        assertThat(printBanner(Duration.ofMillis(1_001))).contains("ttl=1.001s");
    }

    @Test
    void shouldKeepReadableWholeDurationUnits() throws Exception {
        assertThat(printBanner(Duration.ofHours(1))).contains("ttl=1h");
        assertThat(printBanner(Duration.ofMinutes(5))).contains("ttl=5m");
        assertThat(printBanner(Duration.ofSeconds(7))).contains("ttl=7s");
    }

    @Test
    void shouldShowNoneForNonPositiveDuration() throws Exception {
        assertThat(printBanner(Duration.ZERO)).contains("ttl=none");
        assertThat(printBanner(Duration.ofMillis(-1))).contains("ttl=none");
    }

    @Test
    void shouldSkipBannerWhenBannerConfigurationIsMissing() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.setBanner(null);

        assertThat(printBanner(properties)).isEmpty();
    }

    @Test
    void shouldRenderMissingNestedConfigurationWithoutThrowing() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getBanner().setEnabled(true);
        properties.setIdempotent(null);
        properties.getLog().setTrace(null);
        properties.getWeb().setXss(null);

        String banner = printBanner(properties);

        assertThat(banner)
                .contains("idempotent   unavailable (config missing)")
                .contains("trace=unavailable (config missing)")
                .contains("xss=unavailable (config missing)");
    }

    private String printBanner(Duration ttl) throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getBanner().setEnabled(true);
        properties.getCache().setDefaultTtl(ttl);
        return printBanner(properties);
    }

    private String printBanner(VeloProperties properties) throws Exception {
        VeloBannerPrinter printer = new VeloBannerPrinter(properties,
                emptyProvider(), emptyProvider(), emptyProvider());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8.name()));
            printer.afterSingletonsInstantiated();
        } finally {
            System.setOut(original);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
