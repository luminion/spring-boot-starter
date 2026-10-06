package io.github.luminion.velo.lock.support;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonLockHandlerTests {

    @Test
    void shouldUseImmediateNativeWatchdogOverloadAndReleaseOwnedLock() {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("order:1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RedissonLockHandler handler = new RedissonLockHandler(client);
        assertThat(handler.tryLock("order:1")).isTrue();
        handler.unlock("order:1");
        verify(lock).tryLock();
        verify(lock).unlock();
    }

    @Test
    void acquisitionFailureShouldPropagateInsteadOfReportingContention() {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("order:1")).thenReturn(lock);
        IllegalStateException outage = new IllegalStateException("redis unavailable");
        when(lock.tryLock()).thenThrow(outage);
        RedissonLockHandler handler = new RedissonLockHandler(client);

        assertThatThrownBy(() -> handler.tryLock("order:1")).isSameAs(outage);
    }
}
