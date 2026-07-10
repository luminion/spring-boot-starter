package io.github.luminion.velo.log.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void shouldCopySubmitContextToExecutingThread() throws InterruptedException {
        MDC.put("traceId", "trace-abc");
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> seen.set(MDC.get("traceId")));

        // 在另一个线程执行，模拟线程池
        Thread worker = new Thread(decorated);
        worker.start();
        worker.join();

        assertThat(seen.get()).isEqualTo("trace-abc");
    }

    @Test
    void shouldRestoreExecutingThreadContextAfterRun() throws InterruptedException {
        MDC.put("traceId", "submit-value");
        Runnable decorated = decorator.decorate(() -> {
        });

        Thread worker = new Thread(() -> {
            MDC.put("traceId", "worker-original");
            decorated.run();
            // 跑完应还原线程池线程原有值，不被提交上下文污染
            assertThat(MDC.get("traceId")).isEqualTo("worker-original");
        });
        worker.start();
        worker.join();
    }

    @Test
    void shouldClearLeftoverContextWhenSubmitContextEmpty() throws InterruptedException {
        // 提交线程无 MDC，此时 decorate 捕获的是空快照
        MDC.clear();
        AtomicReference<String> during = new AtomicReference<>("sentinel");
        Runnable decorated = decorator.decorate(() -> during.set(MDC.get("traceId")));

        Thread worker = new Thread(() -> {
            // 模拟被复用的线程池线程残留了上一个任务的 MDC
            MDC.put("traceId", "worker-leftover");
            decorated.run();
        });
        worker.start();
        worker.join();

        // 空提交上下文应清掉复用线程的残留值，任务内看到的是 null 而非脏值
        assertThat(during.get()).isNull();
    }
}
