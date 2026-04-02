package io.github.luminion.velo.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeloConfigurationMetadataTest {

    @Test
    void shouldGenerateConfigurationMetadataForCoreProperties() throws IOException {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("META-INF/spring-configuration-metadata.json")) {
            assertNotNull(inputStream);
            String metadata = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(metadata.contains("\"groups\""));
            assertTrue(metadata.contains("velo.core.enabled"));
            assertTrue(metadata.contains("velo.core.fingerprinter-enabled"));
            assertTrue(metadata.contains("velo.cache.default-ttl-seconds"));
            assertTrue(metadata.contains("velo.cache.key-separator"));
            assertTrue(metadata.contains("velo.cache.cache-manager-enabled"));
            assertTrue(metadata.contains("velo.excel.enabled"));
            assertTrue(metadata.contains("velo.excel.converters.enabled"));
            assertTrue(metadata.contains("velo.excel.converters.boolean-converter-enabled"));
            assertTrue(metadata.contains("velo.date-time-format.converters.enabled"));
            assertTrue(metadata.contains("velo.date-time-format.converters.java-util-date-converter-enabled"));
            assertTrue(metadata.contains("velo.jackson.write-integer-as-string"));
            assertTrue(metadata.contains("velo.jackson.builder-customizer-enabled"));
            assertTrue(metadata.contains("velo.jackson.date-time.enabled"));
            assertTrue(metadata.contains("velo.jackson.date-time.serializers-enabled"));
            assertTrue(metadata.contains("velo.jackson.date-time.deserializers-enabled"));
            assertTrue(metadata.contains("velo.jackson.write-unsafe-integer-as-string"));
            assertTrue(metadata.contains("velo.jackson.write-big-decimal-as-string"));
            assertTrue(metadata.contains("velo.jackson.write-floating-point-as-string"));
            assertTrue(metadata.contains("velo.jackson.string-converters.enabled"));
            assertTrue(metadata.contains("velo.log.enabled"));
            assertTrue(metadata.contains("velo.log.slf4j-log-writer-enabled"));
            assertTrue(metadata.contains("velo.jackson.enum-description-enabled"));
            assertTrue(metadata.contains("velo.mybatis-plus.enabled"));
            assertTrue(metadata.contains("velo.mybatis-plus.interceptor-enabled"));
            assertTrue(metadata.contains("velo.date-time-format.enabled"));
            assertTrue(metadata.contains("velo.web.enabled"));
            assertTrue(metadata.contains("velo.web.mvc-configurer-enabled"));
            assertTrue(metadata.contains("velo.web.date-time-formatter-registration-enabled"));
            assertTrue(metadata.contains("velo.web.xss-string-converter-registration-enabled"));
            assertTrue(metadata.contains("velo.redis.string-object-redis-template-enabled"));
            assertTrue(metadata.contains("velo.idempotent.backends.redisson-enabled"));
            assertTrue(metadata.contains("velo.rate-limit.backends.redis-enabled"));
            assertTrue(metadata.contains("velo.lock.backends.jdk-enabled"));
        }
    }
}
