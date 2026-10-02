package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.RedisLockHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        // 重入校验/看门狗续约均为 (script, keys, token, lease) 形式的 2 参脚本调用，默认返回持有权匹配
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any())).thenReturn(1L);
        handler = new RedisLockHandler(redisTemplate);
    }

    @Test
    void shouldAcquireLockViaSetIfAbsentOnFirstLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();

        verify(valueOperations, times(1))
                .setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));

        handler.unlock("order:1");
    }

    @Test
    void shouldReenterByValidatingRedisOwnership() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();
        // 同线程重入：回源校验持有权并按"只延长不缩短"续期，不再直接信任本地栈
        assertThat(handler.lock("order:1", 0, 30000)).isTrue();

        verify(valueOperations, times(1))
                .setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate, times(1))
                .execute(eq(declaredScript("REENTRANT_SCRIPT")), any(List.class), any(), any());

        handler.unlock("order:1");
        handler.unlock("order:1");
        verify(redisTemplate, times(1))
                .execute(eq(declaredScript("RELEASE_SCRIPT")), any(List.class), any());
    }

    @Test
    void shouldRejectReentryWhenRedisOwnershipLost() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any())).thenReturn(0L);

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();
        // 锁已过期并被其他持有者抢占：重入必须失败，且不得信任本地栈短路
        assertThat(handler.lock("order:1", 0, 30000)).isFalse();
        assertThat(handler.lock("order:1", 0, 30000)).isFalse();

        verify(redisTemplate, times(2))
                .execute(eq(declaredScript("REENTRANT_SCRIPT")), any(List.class), any(), any());

        // 外层 unlock 仍按原 token 释放：token 不匹配时脚本为空操作，不会误删他人锁
        handler.unlock("order:1");
        verify(redisTemplate, times(1))
                .execute(eq(declaredScript("RELEASE_SCRIPT")), any(List.class), any());
    }

    @Test
    void shouldFailClosedWhenReentrantCheckThrows() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any()))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();
        // Redis 不可用时无法校验持有权，保守拒绝重入而不是静默放行
        assertThat(handler.lock("order:1", 0, 30000)).isFalse();
    }

    @Test
    void shouldRejectReentryWhenRedisCommandIsDeferred() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any())).thenReturn(null);

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();
        // 返回 null 说明命令被事务/pipeline 排队，持有权判定不可信，按失败处理
        assertThat(handler.lock("order:1", 0, 30000)).isFalse();
    }

    @Test
    void shouldReleaseRedisLockOnlyAtOutermostUnlock() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        handler.lock("order:1", 0, 30000);
        handler.lock("order:1", 0, 30000);

        // 内层 unlock：只递减本地计数，不删 Redis（此时唯一的脚本执行来自重入校验）
        handler.unlock("order:1");
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), any(List.class), any(), any());
        verify(redisTemplate, never()).execute(eq(declaredScript("RELEASE_SCRIPT")), any(List.class), any());

        // 最外层 unlock：真正执行删除脚本
        handler.unlock("order:1");
        verify(redisTemplate, times(1))
                .execute(eq(declaredScript("RELEASE_SCRIPT")), any(List.class), any());
    }

    @Test
    void shouldNotDeadlockOnSameThreadReentrantLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();
        // Redis 侧持有权校验通过（stub 返回匹配），本线程重入应立刻成功
        assertThat(handler.lock("order:1", 0, 30000)).isTrue();

        handler.unlock("order:1");
        handler.unlock("order:1");
    }

    @Test
    void shouldRetryWithinShortWaitTimeout() {
        handler = new RedisLockHandler(redisTemplate, Duration.ofMillis(1));
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false, true);

        assertThat(handler.lock("order:1", 50, 30000)).isTrue();
        verify(valueOperations, times(2))
                .setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));

        handler.unlock("order:1");
    }

    @Test
    void shouldRejectNonPositiveRetryInterval() {
        assertThatThrownBy(() -> new RedisLockHandler(redisTemplate, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retry interval");
    }

    @Test
    void shouldNotCreateThreadLocalStateWhenRedisAcquisitionFails() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> handler.lock("order:1", 0, 30000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis unavailable");

        Field field = RedisLockHandler.class.getDeclaredField("lockValues");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<String, ?>> lockValues = (ThreadLocal<Map<String, ?>>) field.get(handler);
        assertThat(lockValues.get()).isNull();
    }

    @Test
    void shouldUseConfiguredMillisecondLeaseDirectly() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 2)).isTrue();

        verify(valueOperations).setIfAbsent(eq("order:1"), anyString(), eq(2L), eq(TimeUnit.MILLISECONDS));
        handler.unlock("order:1");
    }

    private static RedisScript<Long> declaredScript(String fieldName) throws Exception {
        Field field = RedisLockHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (RedisScript<Long>) field.get(null);
    }

}
