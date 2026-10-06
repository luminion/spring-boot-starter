package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class JdkLockHandlerTests {
    @Test
    void foreignUnlockAndFailedAcquisitionDoNotReleaseOwner() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        assertThat(handler.tryLock("key")).isTrue();
        try {
            CompletableFuture.runAsync(() -> {
                handler.unlock("key");
                assertThat(handler.tryLock("key")).isFalse();
                handler.unlock("key");
                assertThat(handler.tryLock("key")).isFalse();
            }).get(5, TimeUnit.SECONDS);
            assertThat(lockMap(handler)).hasSize(1);
        } finally {
            handler.unlock("key");
        }
        assertThat(lockMap(handler)).isEmpty();
    }

    @Test
    void reentrantOwnershipBlocksAnotherThreadUntilOutermostUnlock() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        assertThat(handler.tryLock("same-key")).isTrue();
        assertThat(handler.tryLock("same-key")).isTrue();
        handler.unlock("same-key");
        assertThat(CompletableFuture.supplyAsync(() -> handler.tryLock("same-key")).join()).isFalse();
        assertThat(lockMap(handler)).hasSize(1);
        handler.unlock("same-key");
        assertThat(lockMap(handler)).isEmpty();
        CompletableFuture.runAsync(() -> {
            assertThat(handler.tryLock("same-key")).isTrue();
            handler.unlock("same-key");
        }).join();
        assertThat(lockMap(handler)).isEmpty();
    }


    @Test
    void shouldRemoveIdleLockStateAfterUnlock() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();

        assertThat(handler.tryLock("demo")).isTrue();
        handler.unlock("demo");

        assertThat(lockMap(handler)).isEmpty();
    }

    @Test
    void uniqueHistoricalKeysShouldNotAccumulate() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        for (int i = 0; i < 10000; i++) {
            String key = "order:" + i;
            assertThat(handler.tryLock(key)).isTrue();
            handler.unlock(key);
        }
        assertThat(lockMap(handler)).isEmpty();
    }

    @Test
    void concurrentStateRemovalShouldPreserveMutualExclusionAndLeaveNoRecords() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        ExecutorService workers = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger acquired = new AtomicInteger();
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int worker = 0; worker < 8; worker++) {
                tasks.add(workers.submit(() -> {
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    for (int attempt = 0; attempt < 2000; attempt++) {
                        if (handler.tryLock("shared")) {
                            try {
                                assertThat(active.incrementAndGet()).isEqualTo(1);
                                acquired.incrementAndGet();
                                assertThat(handler.tryLock("shared")).isTrue();
                                try {
                                    assertThat(active.get()).isEqualTo(1);
                                } finally {
                                    handler.unlock("shared");
                                }
                            } finally {
                                active.decrementAndGet();
                                handler.unlock("shared");
                            }
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(20, TimeUnit.SECONDS);
            }
            assertThat(acquired.get()).isPositive();
            assertThat(active.get()).isZero();
            assertThat(lockMap(handler)).isEmpty();
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> lockMap(JdkLockHandler handler) throws Exception {
        Field field = JdkLockHandler.class.getDeclaredField("lockMap");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(handler);
    }
}
