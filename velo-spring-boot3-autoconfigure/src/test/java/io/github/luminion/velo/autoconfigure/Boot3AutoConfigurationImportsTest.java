package io.github.luminion.velo.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Boot3AutoConfigurationImportsTest {

    @Test
    void shouldExposeNormalizedAutoConfigurationImports() throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            assertNotNull(inputStream);
            String imports = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(imports.contains("io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration"));
            assertTrue(imports.contains("io.github.luminion.velo.lock.VeloLockAutoConfiguration"));
            assertTrue(imports.contains("io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration"));
            assertTrue(imports.contains("io.github.luminion.velo.mybatisplus.VeloMybatisPlusAutoConfiguration"));
            assertTrue(imports.contains("io.github.luminion.velo.redis.VeloRedisAutoConfiguration"));
            assertTrue(imports.contains("io.github.luminion.velo.excel.VeloExcelAutoConfiguration"));
            assertTrue(imports.contains("io.github.luminion.velo.jackson.VeloJacksonAutoConfiguration"));
            assertTrue(!imports.contains("VeloIdempotentConfig"));
            assertTrue(!imports.contains("VeloLockConfig"));
            assertTrue(!imports.contains("VeloRateLimitConfig"));
            assertTrue(!imports.contains("VeloJacksonConfig"));
            assertTrue(!imports.contains("VeloRedisConfig"));
            assertTrue(!imports.contains("VeloMybatisPlusConfig"));
            assertTrue(!imports.contains("WebMvcEnhanceConfigurer"));
        }
    }
}
