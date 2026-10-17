package io.github.luminion.velo.autoconfigure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 Spring Boot 2 模块公开的自动配置条目已使用标准配置类名称。 */
class Boot2AutoConfigurationImportsTest {

    /** 确认自动配置文件包含预期入口且不包含历史配置类名称。 */
    @Test
    void shouldExposeNormalizedSpringFactoriesEntries() throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/spring.factories")) {
            assertNotNull(inputStream);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String factories = new String(output.toByteArray(), StandardCharsets.UTF_8);

            assertTrue(factories.contains("io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.lock.VeloLockAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.mybatisplus.VeloMybatisPlusAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.redis.VeloRedisAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.feign.VeloFeignAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.excel.VeloExcelAutoConfiguration"));
            assertTrue(factories.contains("io.github.luminion.velo.jackson.VeloJacksonAutoConfiguration"));
            assertTrue(!factories.contains("io.github.luminion.velo.webflux."));
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
