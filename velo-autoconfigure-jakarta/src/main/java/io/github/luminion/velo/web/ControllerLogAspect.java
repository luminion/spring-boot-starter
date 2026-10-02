package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.core.util.WebUtils;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerMapping;

/** MVC Controller 调用日志切面。 */
@Aspect
public class ControllerLogAspect implements Ordered {
  private final InvocationLogEngine engine;
  @Getter @Setter private int order = VeloAdvisorOrder.LOG_CONTROLLER;

  public ControllerLogAspect(InvocationLogEngine engine) {
    this.engine = engine;
  }

  @Around(
      "execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController"
          + " *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) ||"
          + " @within(org.springframework.web.bind.annotation.ResponseBody))")
  public Object logControllerInvocation(ProceedingJoinPoint point) throws Throwable {
    String target = "";
    if (WebUtils.isWebContext()) {
      Object pattern =
          WebUtils.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      String path = pattern == null ? WebUtils.getRequestURI() : String.valueOf(pattern);
      target = WebUtils.getRequestIp() + ' ' + WebUtils.getRequestMethod() + ' ' + path;
    }
    HttpServletRequest request = WebUtils.isWebContext() ? WebUtils.getRequest() : null;
    HttpServletResponse response = WebUtils.isWebContext() ? WebUtils.getResponse() : null;
    return engine.invoke(
        InvocationLogSupport.invocation(point, InvocationLogSource.CONTROLLER, target).toBuilder()
            .requestHeaders(request == null ? null : () -> requestHeaders(request))
            .responseHeaders(response == null ? null : () -> responseHeaders(response))
            .build(),
        point::proceed);
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
