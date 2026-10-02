package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class JdkLockHandlerTests {
    @Test
    void reentrantOwnershipBlocksAnotherThreadUntilOutermostUnlock() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();
        assertThat(handler.lock("same-key", 0, 30000)).isTrue();
        assertThat(handler.lock("same-key", 0, 30000)).isTrue();
        handler.unlock("same-key");
        assertThat(CompletableFuture.supplyAsync(() -> handler.lock("same-key", 0, 30000)).join()).isFalse();
        assertThat(lockMap(handler)).hasSize(1);
        handler.unlock("same-key");
        assertThat(lockMap(handler)).isEmpty();
        CompletableFuture.runAsync(() -> {
            assertThat(handler.lock("same-key", 0, 30000)).isTrue();
            handler.unlock("same-key");
        }).join();
        assertThat(lockMap(handler)).isEmpty();
    }


    @Test
    void shouldRemoveIdleLockStateAfterUnlock() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();

        assertThat(handler.lock("demo", 0, 30000)).isTrue();
        handler.unlock("demo");

        assertThat(lockMap(handler)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> lockMap(JdkLockHandler handler) throws Exception {
        Field field = JdkLockHandler.class.getDeclaredField("lockMap");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(handler);
    }
}
