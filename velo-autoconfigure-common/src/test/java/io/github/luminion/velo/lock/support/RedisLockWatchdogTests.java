package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisLockWatchdogTests {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ScheduledExecutorService watchdogExecutor;
    private ScheduledFuture<?> watchdogFuture;
    private RedisLockHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        watchdogExecutor = mock(ScheduledExecutorService.class);
        watchdogFuture = mock(ScheduledFuture.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doReturn(watchdogFuture).when(watchdogExecutor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(),
                eq(TimeUnit.MILLISECONDS));
        handler = new RedisLockHandler(redisTemplate, Duration.ofMillis(1), watchdogExecutor);
    }

    @AfterEach
    void tearDown() {
        handler.close();
    }

    @Test
    void shouldRenewWatchdogLeaseForSynchronousLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any())).thenReturn(1L);

        assertThat(handler.lock("order:1", 0, -1)).isTrue();
        Runnable watchdog = captureWatchdog();
        watchdog.run();

        verify(redisTemplate).execute(any(RedisScript.class), eq(Collections.singletonList("order:1")),
                anyString(), eq("30000"));

        handler.unlock("order:1");
        verify(watchdogFuture).cancel(false);
    }

    @Test
    void shouldKeepPositiveLeaseAsFixedTtlWithoutStartingWatchdog() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(handler.lock("order:1", 0, 30000)).isTrue();

        verify(watchdogExecutor, never()).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(),
                eq(TimeUnit.MILLISECONDS));
        handler.unlock("order:1");
    }

    @Test
    void shouldStopWatchdogWhenRedisLockIsNoLongerOwned() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any())).thenReturn(0L);

        assertThat(handler.lock("order:1", 0, -1)).isTrue();
        captureWatchdog().run();

        verify(watchdogFuture).cancel(false);
        handler.unlock("order:1");
    }

    @Test
    void shouldRenewAndCancelWatchdogForReactiveToken() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any())).thenReturn(1L);

        LockToken token = handler.lockToken("reactive:1", 0, -1).toCompletableFuture().join();
        assertThat(token).isNotNull();
        captureWatchdog().run();

        handler.unlockToken(token).toCompletableFuture().join();
        verify(redisTemplate).execute(any(RedisScript.class), eq(Collections.singletonList("reactive:1")),
                anyString(), eq("30000"));
        verify(watchdogFuture).cancel(false);
    }

    private Runnable captureWatchdog() {
        org.mockito.ArgumentCaptor<Runnable> captor = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(watchdogExecutor).scheduleAtFixedRate(captor.capture(), eq(10000L), eq(10000L),
                eq(TimeUnit.MILLISECONDS));
        return captor.getValue();
    }
}
