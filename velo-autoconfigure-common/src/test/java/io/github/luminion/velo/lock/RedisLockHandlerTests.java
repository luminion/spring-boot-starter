package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.RedisLockHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    private StringRedisTemplate template;
    private ValueOperations<String, String> values;
    private RedisLockHandler handler;
    private TrackingThreadLocal holds;
    private final AtomicReference<String> redisOwner = new AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenAnswer(call -> {
            redisOwner.set(call.getArgument(1));
            return true;
        });
        when(values.get("order:1")).thenAnswer(call -> redisOwner.get());
        handler = new RedisLockHandler(template);
        holds = new TrackingThreadLocal();
        ReflectionTestUtils.setField(handler, "holds", holds);
    }

    @Test
    void defaultTtlShouldBeSixtySeconds() {
        assertThat(handler.tryLock("order:1")).isTrue();
        verify(values).setIfAbsent(eq("order:1"), anyString(), eq(60L), eq(TimeUnit.SECONDS));
        handler.unlock("order:1");
    }

    @Test
    void shouldUseConstructorTtlInSeconds() {
        handler = new RedisLockHandler(template, 15);
        assertThat(handler.tryLock("order:1")).isTrue();
        verify(values).setIfAbsent(eq("order:1"), anyString(), eq(15L), eq(TimeUnit.SECONDS));
        handler.unlock("order:1");
    }

    @Test
    void reentryShouldCheckOwnerWithoutResettingTtlAndReleaseOnlyOutermost() {
        assertThat(handler.tryLock("order:1")).isTrue();
        String owner = redisOwner.get();
        assertThat(handler.tryLock("order:1")).isTrue();
        verify(values, times(1)).setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));
        verify(values).get("order:1");
        handler.unlock("order:1");
        verify(template, never()).execute(any(RedisScript.class), any(List.class), any());
        handler.unlock("order:1");
        verify(template).execute(any(RedisScript.class), eq(Collections.singletonList("order:1")), eq(owner));
        // 外层释放后立即回收线程状态，下一次调用重新走 Redis 获取路径。
        redisOwner.set(null);
        assertThat(handler.tryLock("order:1")).isTrue();
        verify(values, times(2)).setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));
        handler.unlock("order:1");
    }

    @Test
    void expiredOwnerShouldRejectReentryAndUnlockWithOriginalToken() {
        assertThat(handler.tryLock("order:1")).isTrue();
        String originalOwner = redisOwner.get();
        redisOwner.set("new-owner");
        assertThat(handler.tryLock("order:1")).isFalse();
        handler.unlock("order:1");
        verify(template).execute(any(RedisScript.class), eq(Collections.singletonList("order:1")), eq(originalOwner));
    }

    @Test
    void redisFailureOnReentryShouldPropagate() {
        assertThat(handler.tryLock("order:1")).isTrue();
        when(values.get("order:1")).thenThrow(new IllegalStateException("redis unavailable"));
        assertThatThrownBy(() -> handler.tryLock("order:1"))
                .isInstanceOf(IllegalStateException.class).hasMessage("redis unavailable");
        handler.unlock("order:1");
    }

    @Test
    void deferredAcquisitionShouldThrowAndNotRetainOwner() {
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(null, true);
        assertThatThrownBy(() -> handler.tryLock("order:1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("transaction or pipeline");
        assertThat(holds.present).isFalse();
        assertThat(handler.tryLock("order:1")).isTrue();
        verify(values, never()).get(anyString());
        handler.unlock("order:1");
    }

    @Test
    void busyLockShouldAttemptOnlyOnceWithoutRetainingThreadState() {
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false, true);
        assertThat(handler.tryLock("order:1")).isFalse();
        verify(values, times(1)).setIfAbsent(eq("order:1"), anyString(), anyLong(), any(TimeUnit.class));
        assertThat(holds.present).isFalse();
    }

    @Test
    void shouldRejectInvalidTtlOrMissingTemplate() {
        for (long ttl : new long[]{0, -1}) {
            assertThatThrownBy(() -> new RedisLockHandler(template, ttl))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TTL seconds");
        }
        assertThatThrownBy(() -> new RedisLockHandler(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("template");
    }

    @Test
    void initialAcquisitionFailureShouldNotLeaveThreadState() {
        IllegalStateException outage = new IllegalStateException("redis unavailable");
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenThrow(outage);
        assertThatThrownBy(() -> handler.tryLock("order:1")).isSameAs(outage);
        assertThat(holds.present).isFalse();
    }

    @Test
    void unlockWithoutOwnershipShouldNotCreateThreadState() {
        handler.unlock("missing");
        assertThat(holds.present).isFalse();
        verify(template, never()).execute(any(RedisScript.class), any(List.class), any());
    }

    @Test
    void threadStateShouldContainOnlyActiveKeysAndSurvivePartialUnlock() {
        assertThat(handler.tryLock("order:1")).isTrue();
        assertThat(handler.tryLock("order:1")).isTrue();
        assertThat(handler.tryLock("order:2")).isTrue();
        handler.unlock("order:1");
        assertThat(holds.value).containsOnlyKeys("order:1", "order:2");
        handler.unlock("order:1");
        assertThat(holds.value).containsOnlyKeys("order:2");
        handler.unlock("missing");
        assertThat(holds.value).containsOnlyKeys("order:2");
        handler.unlock("order:2");
        assertThat(holds.present).isFalse();
    }

    @Test
    void releaseFailureShouldStillClearThreadState() {
        assertThat(handler.tryLock("order:1")).isTrue();
        when(template.execute(any(RedisScript.class), any(List.class), any()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        handler.unlock("order:1");
        assertThat(holds.present).isFalse();
    }

    @Test
    void uniqueHistoricalKeysShouldNotAccumulateInThreadState() {
        for (int i = 0; i < 1000; i++) {
            String key = "order:" + i;
            assertThat(handler.tryLock(key)).isTrue();
            handler.unlock(key);
            assertThat(holds.present).isFalse();
        }
    }

    private static class TrackingThreadLocal extends ThreadLocal<Map<String, ?>> {
        private boolean present;
        private Map<String, ?> value;

        @Override
        protected Map<String, ?> initialValue() {
            present = true;
            return null;
        }

        @Override
        public void set(Map<String, ?> value) {
            present = true;
            this.value = value;
            super.set(value);
        }

        @Override
        public void remove() {
            present = false;
            value = null;
            super.remove();
        }
    }
}
