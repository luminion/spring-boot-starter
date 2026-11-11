package io.github.luminion.velo.web;

import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.core.util.WebUtils;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

/**
 * MVC Controller 调用日志拦截器。
 * <p>
 * 记录 Controller 调用及其请求、响应头，并将实际调用交由日志引擎执行。
 * 耗时只统计 Controller 方法本身，不包含过滤器、拦截器和响应序列化；日志中也没有最终 HTTP 状态码。
 */
public class ControllerLogAspect implements MethodInterceptor {
    private final InvocationLogEngine engine;

    /**
     * 创建 Controller 调用日志拦截器。
     *
     * @param engine 调用日志引擎
     */
    public ControllerLogAspect(InvocationLogEngine engine) {
        this.engine = engine;
    }

    /**
     * Controller 入口切点：类型或方法上声明了 {@link ResponseBody}（含 {@code @RestController}）的 public 方法。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        return AnnotatedMethodPointcut.forPublicMethodsOrTypes(ResponseBody.class);
    }

    /**
     * 记录 Controller 调用，并委托日志引擎执行原调用。
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        boolean webContext = WebUtils.isWebContext();
        String path = null;
        if (webContext) {
            Object pattern = WebUtils.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            path = pattern == null ? WebUtils.getRequestURI() : String.valueOf(pattern);
        }
        InvocationLogContext context = InvocationLogContext.builder()
                .clientIp(webContext ? WebUtils.getRequestIp() : null)
                .httpMethod(webContext ? WebUtils.getRequestMethod() : null).path(path).build();
        String target = invocation.getMethod().getName() + "()";
        HttpServletRequest request = webContext ? WebUtils.getRequest() : null;
        HttpServletResponse response = webContext ? WebUtils.getResponse() : null;
        LogInvocation logInvocation = InvocationLogSupport.invocation(invocation, InvocationLogSource.CONTROLLER, target)
                .toBuilder()
                .context(() -> context)
                .requestHeaders(request == null ? null : () -> requestHeaders(request))
                .responseHeaders(response == null ? null : () -> responseHeaders(response))
                .build();
        return engine.invoke(logInvocation, invocation::proceed);
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
