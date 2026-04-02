package io.github.luminion.velo.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Boot2AutoConfigurationImportsTest {

    @Test
    void shouldExposeNormalizedSpringFactoriesEntries() throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/spring.factories")) {
            assertNotNull(inputStream);
            String factories = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(factories.contains("io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.lock.VeloLockAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.mybatisplus.VeloMybatisPlusAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.redis.VeloRedisAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.excel.VeloExcelAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.jackson.VeloJacksonAutoConfiguration"));
            assertTrue(!factories.contains("VeloIdempotentConfig"));
            assertTrue(!factories.contains("VeloLockConfig"));
            assertTrue(!factories.contains("VeloRateLimitConfig"));
            assertTrue(!factories.contains("VeloJacksonConfig"));
            assertTrue(!factories.contains("VeloRedisConfig"));
            assertTrue(!factories.contains("VeloMybatisPlusConfig"));
            assertTrue(!factories.contains("WebMvcEnhanceConfigurer"));
        }
    }
}
