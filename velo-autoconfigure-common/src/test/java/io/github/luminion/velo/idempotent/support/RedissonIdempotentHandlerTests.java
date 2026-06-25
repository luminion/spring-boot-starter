package io.github.luminion.velo.idempotent.support;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonIdempotentHandlerTests {

    @Test
    void shouldConvertTimeoutToDurationWithoutJavaNineTimeUnitApi() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket();
        when(redissonClient.<String>getBucket("demo")).thenReturn(bucket);
        when(bucket.setIfAbsent("token-1", Duration.ofMillis(1500L))).thenReturn(true);

        RedissonIdempotentHandler handler = new RedissonIdempotentHandler(redissonClient);
        boolean recorded = handler.tryRecord("demo", "token-1", 1500L, TimeUnit.MILLISECONDS);

        assertThat(recorded).isTrue();
        verify(bucket).setIfAbsent("token-1", Duration.ofMillis(1500L));
    }

    @Test
    void shouldRemoveOnlyWhenTokenMatches() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket();
        when(redissonClient.<String>getBucket("demo")).thenReturn(bucket);

        RedissonIdempotentHandler handler = new RedissonIdempotentHandler(redissonClient);
        handler.removeIfMatch("demo", "token-1");

        // compareAndSet(token, null) 等价于"值匹配才删除"
        verify(bucket).compareAndSet("token-1", null);
    }

    @SuppressWarnings("unchecked")
    private static RBucket<String> mockBucket() {
        return mock(RBucket.class);
    }
}
