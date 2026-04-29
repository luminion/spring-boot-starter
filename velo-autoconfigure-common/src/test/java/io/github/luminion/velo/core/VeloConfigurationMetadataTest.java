package io.github.luminion.velo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
            assertFalse(metadata.contains("velo.core.enabled"));
            assertFalse(metadata.contains("velo.core.fingerprinter-enabled"));
            assertFalse(metadata.contains("velo.core.naming-suffix-strategy-enabled"));
            assertFalse(metadata.contains("velo.core.enum-field-convention-enabled"));
            assertFalse(metadata.contains("velo.core.json-processor-provider-enabled"));
            assertFalse(metadata.contains("velo.core.enum-code-fields"));
            assertFalse(metadata.contains("velo.core.enum-desc-fields"));
            assertTrue(metadata.contains("velo.cache.default-ttl"));
            assertTrue(metadata.contains("velo.cache.prefix"));
            assertTrue(metadata.contains("velo.cache.separator"));
            assertTrue(metadata.contains("velo.cache.ttl"));
            assertFalse(metadata.contains("velo.cache.key-prefix"));
            assertFalse(metadata.contains("velo.cache.key-separator"));
            assertFalse(metadata.contains("velo.cache.ttl-map"));
            assertFalse(metadata.contains("velo.cache.cache-manager-enabled"));
            assertFalse(metadata.contains("velo.cache.redis-cache-configuration-enabled"));
            assertFalse(metadata.contains("velo.cache.redis-cache-time-map-provider-enabled"));
            assertTrue(metadata.contains("velo.excel.enabled"));
            assertTrue(metadata.contains("velo.excel.converters.enabled"));
            assertTrue(metadata.contains("velo.excel.converters.boolean-enabled"));
            assertFalse(metadata.contains("velo.excel.converters.boolean-converter-enabled"));
            assertTrue(metadata.contains("velo.date-time-format.converters.enabled"));
            assertFalse(metadata.contains("velo.date-time-format.converters.java-util-date-converter-enabled"));
            assertFalse(metadata.contains("velo.date-time-format.converters.local-date-time-converter-enabled"));
            assertFalse(metadata.contains("velo.date-time-format.converters.local-date-converter-enabled"));
            assertFalse(metadata.contains("velo.date-time-format.converters.local-time-converter-enabled"));
            assertFalse(metadata.contains("velo.jackson.builder-customizer-enabled"));
            assertFalse(metadata.contains("velo.jackson.redis-serializer-enabled"));
            assertTrue(metadata.contains("velo.jackson.date-time-enabled"));
            assertFalse(metadata.contains("velo.jackson.date-time.enabled"));
            assertFalse(metadata.contains("velo.jackson.date-time.java-util-date-enabled"));
            assertFalse(metadata.contains("velo.jackson.date-time.serializers-enabled"));
            assertFalse(metadata.contains("velo.jackson.date-time.deserializers-enabled"));
            assertTrue(metadata.contains("velo.jackson.unsafe-integer-as-string"));
            assertTrue(metadata.contains("velo.jackson.big-decimal-as-string"));
            assertTrue(metadata.contains("velo.jackson.floating-as-string"));
            assertTrue(metadata.contains("velo.jackson.string-converter-enabled"));
            assertFalse(metadata.contains("velo.jackson.string-converter.enabled"));
            assertFalse(metadata.contains("velo.jackson.write-unsafe-integer-as-string"));
            assertFalse(metadata.contains("velo.jackson.write-big-decimal-as-string"));
            assertFalse(metadata.contains("velo.jackson.write-floating-point-as-string"));
            assertFalse(metadata.contains("velo.jackson.string-converters.enabled"));
            assertFalse(metadata.contains("velo.jackson.string-converters.serializer-enabled"));
            assertFalse(metadata.contains("velo.jackson.string-converters.deserializer-enabled"));
            assertTrue(metadata.contains("velo.log.enabled"));
            assertTrue(metadata.contains("velo.log.level"));
            assertFalse(metadata.contains("velo.log.slf4j-log-writer-enabled"));
            assertFalse(metadata.contains("velo.log.args-enabled"));
            assertFalse(metadata.contains("velo.log.result-enabled"));
            assertFalse(metadata.contains("velo.log.error-enabled"));
            assertFalse(metadata.contains("velo.log.slow-enabled"));
            assertFalse(metadata.contains("velo.log.args-aspect-enabled"));
            assertFalse(metadata.contains("velo.log.result-aspect-enabled"));
            assertFalse(metadata.contains("velo.log.error-aspect-enabled"));
            assertFalse(metadata.contains("velo.log.slow-aspect-enabled"));
            assertTrue(metadata.contains("velo.jackson.enum-desc-enabled"));
            assertTrue(metadata.contains("velo.jackson.enum-name-suffix"));
            assertTrue(metadata.contains("velo.jackson.enum-mappings"));
            assertFalse(metadata.contains("velo.jackson.enum-fields"));
            assertFalse(metadata.contains("velo.jackson.enum-description-enabled"));
            assertTrue(metadata.contains("velo.mybatis-plus.enabled"));
            assertTrue(metadata.contains("velo.mybatis-plus.pagination-enabled"));
            assertTrue(metadata.contains("velo.mybatis-plus.optimistic-locker-enabled"));
            assertTrue(metadata.contains("velo.mybatis-plus.block-attack-enabled"));
            assertFalse(metadata.contains("velo.mybatis-plus.interceptor-enabled"));
            assertFalse(metadata.contains("velo.mybatis-plus.pagination-inner-interceptor-enabled"));
            assertFalse(metadata.contains("velo.mybatis-plus.optimistic-locker-inner-interceptor-enabled"));
            assertFalse(metadata.contains("velo.mybatis-plus.block-attack-inner-interceptor-enabled"));
            assertTrue(metadata.contains("velo.date-time-format.enabled"));
            assertTrue(metadata.contains("velo.web.enabled"));
            assertFalse(metadata.contains("velo.web.mvc-configurer-enabled"));
            assertFalse(metadata.contains("velo.web.date-time-formatter-registration-enabled"));
            assertFalse(metadata.contains("velo.web.xss.cleaner-enabled"));
            assertFalse(metadata.contains("velo.web.xss.string-converter-enabled"));
            assertTrue(metadata.contains("velo.web.xss.strategy"));
            assertTrue(metadata.contains("velo.web.request-logging-enabled"));
            assertFalse(metadata.contains("velo.web.request-logging.enabled"));
            assertFalse(metadata.contains("velo.web.request-logging.include-client-info"));
            assertFalse(metadata.contains("velo.web.request-logging.include-query-string"));
            assertFalse(metadata.contains("velo.web.request-logging.include-payload"));
            assertFalse(metadata.contains("velo.web.request-logging.max-payload-length"));
            assertFalse(metadata.contains("velo.redis.string-object-redis-template-enabled"));
            assertFalse(metadata.contains("velo.redis.redis-template-enabled"));
            assertTrue(metadata.contains("velo.idempotent.backend"));
            assertTrue(metadata.contains("velo.idempotent.prefix"));
            assertFalse(metadata.contains("velo.idempotent.key-prefix"));
            assertTrue(metadata.contains("velo.rate-limit.backend"));
            assertTrue(metadata.contains("velo.rate-limit.prefix"));
            assertFalse(metadata.contains("velo.rate-limit.key-prefix"));
            assertTrue(metadata.contains("velo.lock.backend"));
            assertTrue(metadata.contains("velo.lock.prefix"));
            assertFalse(metadata.contains("velo.lock.key-prefix"));
        }
    }
}
