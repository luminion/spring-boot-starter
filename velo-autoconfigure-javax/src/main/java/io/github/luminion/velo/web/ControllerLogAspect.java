package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.util.WebUtils;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerMapping;

/**
 * MVC Controller 调用日志切面。
 *
 * <p>记录 Controller 调用及其请求、响应头，并将实际调用交由日志引擎执行。</p>
 */
@Aspect
public class ControllerLogAspect implements Ordered {
    private final InvocationLogEngine engine;

    @Getter
    private final int order;

    /**
     * 使用默认的 Controller 日志切面顺序创建切面。
     */
    public ControllerLogAspect(InvocationLogEngine engine) {
        this(engine, VeloAdvisorOrder.LOG_CONTROLLER);
    }

    /**
     * 使用指定的日志引擎和切面顺序创建切面。
     *
     * @param engine 调用日志引擎
     * @param order 切面执行顺序
     */
    public ControllerLogAspect(InvocationLogEngine engine, int order) {
        this.engine = engine;
        this.order = order;
    }

    /**
     * 记录 Controller 调用，并委托日志引擎执行原调用。
     */
    @Around(
            "execution(" +
                    "public * *(..))"
                    + " && (within(@org.springframework.web.bind.annotation.RestController *) "
                    + "|| @annotation(org.springframework.web.bind.annotation.ResponseBody)"
                    + " || @within(org.springframework.web.bind.annotation.ResponseBody)" +
                    ")")
    public Object logControllerInvocation(ProceedingJoinPoint point) throws Throwable {
        boolean webContext = WebUtils.isWebContext();
        String path = null;
        if (webContext) {
            Object pattern = WebUtils.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            path = pattern == null ? WebUtils.getRequestURI() : String.valueOf(pattern);
        }
        InvocationLogContext context = InvocationLogContext.builder()
                .clientIp(webContext ? WebUtils.getRequestIp() : null)
                .httpMethod(webContext ? WebUtils.getRequestMethod() : null).path(path).build();
        String target = point.getSignature().getName() + "()";
        HttpServletRequest request = webContext ? WebUtils.getRequest() : null;
        HttpServletResponse response = webContext ? WebUtils.getResponse() : null;
        return engine.invoke(
                InvocationLogSupport.invocation(point, InvocationLogSource.CONTROLLER, target).toBuilder()
                        .context(() -> context)
                        .requestHeaders(request == null ? null : () -> requestHeaders(request))
                        .responseHeaders(response == null ? null : () -> responseHeaders(response))
                        .build(),
                point::proceed);
    }

    /**
     * 获取当前请求的全部请求头。
     */
    private Map<String, List<String>> requestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }

    /**
     * 获取当前响应的全部响应头。
     */
    private Map<String, List<String>> responseHeaders(HttpServletResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, new ArrayList<>(response.getHeaders(name)));
        }
        return headers;
    }
}
