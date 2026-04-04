package io.github.luminion.velo.lock;

import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JdkLockHandlerTests {

    @Test
    void shouldRemoveIdleLockStateAfterUnlock() throws Exception {
        JdkLockHandler handler = new JdkLockHandler();

        assertThat(handler.lock("demo", 0, 30, TimeUnit.SECONDS)).isTrue();
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
