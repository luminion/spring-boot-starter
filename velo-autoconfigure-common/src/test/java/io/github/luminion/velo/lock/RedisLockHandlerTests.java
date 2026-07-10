package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.RedisLockHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisLockHandlerTests {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisLockHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        handler = new RedisLockHandler(redisTemplate);
    }

    @Test
    void shouldAcquireLockViaSetIfAbsentOnFirstLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30, TimeUnit.SECONDS)).isTrue();

        verify(valueOperations, times(1))
                .setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));

        handler.unlock("order:1");
    }

    @Test
    void shouldReenterWithoutHittingRedisWhenSameThreadLocksAgain() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30, TimeUnit.SECONDS)).isTrue();
        // 同线程重入：不应再次访问 Redis
        assertThat(handler.lock("order:1", 0, 30, TimeUnit.SECONDS)).isTrue();

        verify(valueOperations, times(1))
                .setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));

        handler.unlock("order:1");
        handler.unlock("order:1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReleaseRedisLockOnlyAtOutermostUnlock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        handler.lock("order:1", 0, 30, TimeUnit.SECONDS);
        handler.lock("order:1", 0, 30, TimeUnit.SECONDS);

        // 内层 unlock：只递减本地计数，不删 Redis
        handler.unlock("order:1");
        verify(redisTemplate, never()).execute(any(RedisScript.class), any(List.class), any());

        // 最外层 unlock：真正执行删除脚本
        handler.unlock("order:1");
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), any(List.class), any());
    }

    @Test
    void shouldNotDeadlockOnSameThreadReentrantLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30, TimeUnit.SECONDS)).isTrue();
        // 即便首次持有后 Redis 已被占用，本线程重入仍应立刻成功（走本地计数分支）
        assertThat(handler.lock("order:1", 0, 30, TimeUnit.SECONDS)).isTrue();

        handler.unlock("order:1");
        handler.unlock("order:1");
    }

    @Test
    void shouldRetryWithinShortWaitTimeout() {
        handler = new RedisLockHandler(redisTemplate, Duration.ofMillis(1));
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false, true);

        assertThat(handler.lock("order:1", 50, 30, TimeUnit.MILLISECONDS)).isTrue();
        verify(valueOperations, times(2))
                .setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));

        handler.unlock("order:1");
    }

    @Test
    void shouldRejectNonPositiveRetryInterval() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new RedisLockHandler(redisTemplate, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retry interval");
    }
}
