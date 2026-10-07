package io.github.luminion.velo.cache;

import io.github.luminion.velo.VeloProperties;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证清空策略通过原生连接执行，且未知工厂不被强制套用 SCAN。 */
class VeloCacheConfigurationTest {
    @Test
    void shouldUseNativeScanForLettuce() {
        LettuceConnectionFactory factory = mock(LettuceConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        Cursor<byte[]> cursor = mock(Cursor.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.scan(any(ScanOptions.class))).thenReturn(cursor);
        clear(factory);
        verify(connection).scan(any(ScanOptions.class));
        verify(connection, never()).keys(any(byte[].class));
    }

    @Test
    void shouldKeepNativeKeysForUnknownConnectionFactories() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.keys(any(byte[].class))).thenReturn(Collections.emptySet());
        clear(factory);
        verify(connection).keys(any(byte[].class));
        verify(connection, never()).scan(any(ScanOptions.class));
    }

    private void clear(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig();
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(factory).cacheDefaults(defaults);
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(factory,
                VeloCacheConfiguration.batchStrategy(factory));
        VeloCacheConfiguration.customizeBuilder(builder, writer, defaults,
                new RedisCacheTimeMapProvider(Collections.emptyMap()), new VeloProperties());
        RedisCacheManager manager = builder.build();
        manager.afterPropertiesSet();
        manager.getCache("users").clear();
    }
}
