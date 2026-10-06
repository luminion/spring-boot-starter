package io.github.luminion.velo.ratelimit.support;

import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonRateLimitHandlerTests {

    @Test
    void shouldInitializeLimiterWithQpsAndRefreshTtl() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("demo")).thenReturn(rateLimiter);
        when(rateLimiter.getConfig()).thenReturn(new RateLimiterConfig(RateType.OVERALL, 1000L, 2L));
        when(rateLimiter.tryAcquire()).thenReturn(true);

        RedissonRateLimitHandler handler = new RedissonRateLimitHandler(redissonClient);
        boolean acquired = handler.tryAcquire("demo", 2);

        assertThat(acquired).isTrue();
        verify(rateLimiter).trySetRate(
                RateType.OVERALL,
                2L,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
        verify(rateLimiter, never()).setRate(any(), anyLong(), any(Duration.class), any(Duration.class));
        verify(rateLimiter).expire(Duration.ofSeconds(1));
    }

    @Test
    void shouldRefreshLimiterWhenStoredConfigDoesNotMatchRequest() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("demo")).thenReturn(rateLimiter);
        when(rateLimiter.getConfig()).thenReturn(new RateLimiterConfig(RateType.OVERALL, 1000L, 1L));
        when(rateLimiter.tryAcquire()).thenReturn(true);

        RedissonRateLimitHandler handler = new RedissonRateLimitHandler(redissonClient);
        boolean acquired = handler.tryAcquire("demo", 2);

        assertThat(acquired).isTrue();
        verify(rateLimiter).setRate(
                RateType.OVERALL,
                2L,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
        verify(rateLimiter).expire(Duration.ofSeconds(1));
    }
}
