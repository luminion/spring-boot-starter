package io.github.luminion.starter.log.support;

import io.github.luminion.starter.core.util.WebUtils;
import org.slf4j.event.Level;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 带有 HTTP 请求信息的日志写入器
 */
public class RequestLogWriter extends Slf4JLogWriter {

    public RequestLogWriter(Level level) {
        super(level);
    }

    @Override
    protected void log(Level level, String format, Object... arguments) {
        super.log(level, getRequestInfo() + " " + format, arguments);
    }

    protected String getRequestInfo() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return "[N/A]";
        }
        String requestIp = WebUtils.getRequestIp();
        if (requestIp == null) {
            requestIp = "unknown-ip";
        }
        String requestMethod = WebUtils.getRequestMethod();
        String uri = WebUtils.getRequestURI();
        return String.format("%s %s %s -", requestIp, requestMethod, uri);
    }
}
