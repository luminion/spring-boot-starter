//package io.github.luminion.autoconfigure.aop.support.logging;
//
//import io.github.luminion.autoconfigure.servlet.WebContextHolder;
//import org.slf4j.event.Level;
//import org.springframework.web.context.request.RequestAttributes;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
///**
// * An extension of Slf4jLogWriter that prepends request information (IP, Method, URI) to log messages.
// *
// * @author luminion
// */
//public class RequestLogWriter extends Slf4jLogWriter {
//
//    public RequestLogWriter(Level level) {
//        super(level);
//    }
//
//    @Override
//    protected void log(Level level, String format, Object... arguments) {
//        super.log(level, getRequestInfo() + " " + format, arguments);
//    }
//
//    /**
//     * Formats the request information part of the log.
//     *
//     * @return A string like "127.0.0.1 GET /api/users -", or "[N/A]" if not in a request context.
//     */
//    protected String getRequestInfo() {
//        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
//        if (!(attributes instanceof ServletRequestAttributes)) {
//            return "[N/A]";
//        }
//        String requestIp = WebContextHolder.getRequestIp();
//        if (requestIp == null){
//            requestIp = "unknown-ip";
//        }
//        String requestMethod = WebContextHolder.getRequestMethod();
//        String uri = WebContextHolder.getRequestURI();
//        return String.format("%s %s %s -", requestIp, requestMethod, uri);
//    }
//}
