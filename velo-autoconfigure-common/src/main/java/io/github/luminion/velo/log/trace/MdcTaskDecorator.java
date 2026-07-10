package io.github.luminion.velo.log.trace;

import org.springframework.core.task.TaskDecorator;
import org.slf4j.MDC;

import java.util.Map;

/**
 * 把提交线程的 MDC(含 traceId)复制到执行线程，使 {@code @Async} 与 Spring 线程池任务里的日志也能延续 traceId。
 * <p>
 * 纯 SLF4J + Spring core 实现，不引入任何链路追踪依赖；注册为 Bean 后 Spring Boot 会自动套到默认任务线程池。
 * 仅覆盖 Spring 管理的线程池，裸 {@code new Thread()} / {@code CompletableFuture} 公共池等 Spring 触达不到的场景不在范围内。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // decorate 在提交线程执行，这里抓取当前 MDC 快照
        Map<String, String> submitContext = MDC.getCopyOfContextMap();
        return () -> {
            // 线程池线程可能被复用，先存执行线程原有 MDC，跑完还原，避免跨任务污染
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (submitContext != null) {
                MDC.setContextMap(submitContext);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
