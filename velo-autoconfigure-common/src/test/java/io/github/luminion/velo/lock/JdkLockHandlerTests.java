package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JdkLockHandlerTests {

    @Test
    void shouldRemoveIdleLockStateAfterUnlock() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();

        assertThat(handler.lock("demo", 0, 30000)).isTrue();
        handler.unlock("demo");

        assertThat(lockMap(handler)).isEmpty();
    }

    @Test
    void shouldReleaseReactiveTokenFromAnotherThread() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();

        LockToken token = handler.lockToken("reactive", 0, 30000).toCompletableFuture().get(1, TimeUnit.SECONDS);
        assertThat(token).isNotNull();
        assertThat(handler.lock("reactive", 0, 30000)).isFalse();

        CompletableFuture.runAsync(() -> handler.unlockToken(token).toCompletableFuture().join()).join();

        assertThat(handler.lock("reactive", 0, 30000)).isTrue();
        handler.unlock("reactive");
        assertThat(lockMap(handler)).isEmpty();
    }

    @Test
    void shouldKeepSynchronousAndReactiveLocksMutuallyExclusive() {
        JdkLockHandler handler = new JdkLockHandler();

        assertThat(handler.lock("same-key", 0, 30000)).isTrue();
        assertThat(handler.lockToken("same-key", 0, 30000).toCompletableFuture().join()).isNull();
        handler.unlock("same-key");

        LockToken token = handler.lockToken("same-key", 0, 30000).toCompletableFuture().join();
        assertThat(token).isNotNull();
        assertThat(handler.lock("same-key", 0, 30000)).isFalse();
        handler.unlockToken(token).toCompletableFuture().join();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> lockMap(JdkLockHandler handler) throws Exception {
        Field field = JdkLockHandler.class.getDeclaredField("lockMap");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(handler);
    }
}
