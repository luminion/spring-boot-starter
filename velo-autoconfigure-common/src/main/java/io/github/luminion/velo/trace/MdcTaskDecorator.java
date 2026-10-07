package io.github.luminion.velo.trace;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * 默认执行器的 MDC 传播；没有提交方 traceId 时，每次任务生成一个。
 */
public class MdcTaskDecorator implements TaskDecorator {
    private final String traceKey;
    private final TraceContextResolver resolver;

    public MdcTaskDecorator() {
        this("traceId");
    }

    public MdcTaskDecorator(String traceKey) {
        this(traceKey, new W3cTraceContextResolver());
    }

    public MdcTaskDecorator(String traceKey, TraceContextResolver resolver) {
        this.traceKey = traceKey;
        this.resolver = resolver;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submitted = MDC.getCopyOfContextMap();
        TraceData captured = TraceContext.current();
        TraceData submittedData =
                captured != null
                        && submitted != null
                        && captured.getTraceId().equals(submitted.get(traceKey))
                        ? captured
                        : null;
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try (TraceContext.Scope snapshot = TraceContext.install(traceKey, submittedData, true)) {
                if (submitted == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(submitted);
                }
                // 线程池只携带链路快照，不复制或继承 Servlet 请求、响应。
                try (TraceContext.Scope scope =
                             CurrentRequestHeaders.withoutRequest(
                                     () -> TraceContext.open(traceKey, true, resolver))) {
                    runnable.run();
                }
            } finally {
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        };
    }
}
