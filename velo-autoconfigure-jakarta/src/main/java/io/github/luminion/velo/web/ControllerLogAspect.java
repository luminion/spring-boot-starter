package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.util.WebUtils;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogSource;
import io.github.luminion.velo.log.core.InvocationLogSupport;
import io.github.luminion.velo.log.core.LogInvocation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 为 MVC 控制器调用创建统一调用日志，并记录请求和响应头。
 */
@Aspect
public class ControllerLogAspect implements Ordered {
    private final InvocationLogEngine engine;
    @Getter
    private final int order;

    /**
     * 使用默认的控制器日志切面顺序创建切面。
     *
     * @param engine 调用日志引擎
     */
    public ControllerLogAspect(InvocationLogEngine engine) {
        this(engine, VeloAdvisorOrder.LOG_CONTROLLER);
    }

    /**
     * 使用指定顺序创建控制器日志切面。
     *
     * @param engine 调用日志引擎
     * @param order 切面执行顺序
     */
    public ControllerLogAspect(InvocationLogEngine engine, int order) {
        this.engine = engine;
        this.order = order;
    }

    /**
     * 环绕执行控制器方法，并将请求上下文加入调用日志。
     *
     * @param point 当前控制器方法的连接点
     * @return 控制器方法的执行结果
     * @throws Throwable 控制器方法执行时抛出的异常
     */
    @Around("execution(" +
            "public * *(..))"
            + " && (within(@org.springframework.web.bind.annotation.RestController *) "
            + "|| @annotation(org.springframework.web.bind.annotation.ResponseBody)"
            + " || @within(org.springframework.web.bind.annotation.ResponseBody)" +
            ")")
    public Object logControllerInvocation(ProceedingJoinPoint point) throws Throwable {
        String target = "";
        if (WebUtils.isWebContext()) {
            Object pattern = WebUtils.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String path = pattern == null ? WebUtils.getRequestURI() : String.valueOf(pattern);
            target = WebUtils.getRequestIp() + ' ' + WebUtils.getRequestMethod() + ' ' + path;
        }
        HttpServletRequest request = WebUtils.isWebContext() ? WebUtils.getRequest() : null;
        HttpServletResponse response = WebUtils.isWebContext() ? WebUtils.getResponse() : null;
        var logInvocation = InvocationLogSupport.invocation(point, InvocationLogSource.CONTROLLER, target)
                .toBuilder()
                .requestHeaders(request == null ? null : () -> requestHeaders(request))
                .responseHeaders(response == null ? null : () -> responseHeaders(response))
                .build();
        return engine.invoke(logInvocation, point::proceed);
    }

    private Map<String, List<String>> requestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }

    private Map<String, List<String>> responseHeaders(HttpServletResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, new ArrayList<>(response.getHeaders(name)));
        }
        return headers;
    }
}
