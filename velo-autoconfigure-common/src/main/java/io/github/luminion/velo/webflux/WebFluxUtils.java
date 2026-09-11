package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.util.WebUtilsSupport;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

/**
 * WebFlux 请求和响应工具类。
 *
 * <p>WebFlux 没有 Servlet 的当前请求线程上下文，因此所有方法都显式接收
 * {@link ServerWebExchange}。请求体、Session、Principal 和响应写入均保持响应式返回类型，
 * 不会通过阻塞调用破坏 WebFlux 的非阻塞模型。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
public final class WebFluxUtils {

    private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");

    private static final MediaType TEXT_UTF8 = MediaType.parseMediaType("text/plain;charset=UTF-8");

    private static final MediaType XML_UTF8 = MediaType.parseMediaType("application/xml;charset=UTF-8");

    private WebFluxUtils() {
    }

    /**
     * 判断显式传入的 exchange 是否为可用的 WebFlux 请求上下文。
     *
     * @param exchange 当前 exchange
     * @return exchange 非空时返回 {@code true}
     */
    public static boolean isWebContext(ServerWebExchange exchange) {
        return exchange != null;
    }

    /**
     * 获取请求对象。
     *
     * @param exchange 当前 exchange
     * @return 请求对象
     */
    public static ServerHttpRequest getRequest(ServerWebExchange exchange) {
        return requireExchange(exchange).getRequest();
    }

    /**
     * 获取响应对象。
     *
     * @param exchange 当前 exchange
     * @return 响应对象
     */
    public static ServerHttpResponse getResponse(ServerWebExchange exchange) {
        return requireExchange(exchange).getResponse();
    }

    /**
     * 获取 WebSession。Session 创建和读取都是异步操作。
     *
     * @param exchange 当前 exchange
     * @return WebSession 响应式结果
     */
    public static Mono<WebSession> getSession(ServerWebExchange exchange) {
        return requireExchange(exchange).getSession();
    }

    /**
     * 获取当前请求的 Principal。
     *
     * @param exchange 当前 exchange
     * @return Principal 响应式结果
     */
    public static Mono<? extends Principal> getPrincipal(ServerWebExchange exchange) {
        return requireExchange(exchange).getPrincipal();
    }

    /**
     * 获取请求 URI 路径，不包含查询参数。
     *
     * @param exchange 当前 exchange
     * @return 请求路径
     */
    public static String getRequestURI(ServerWebExchange exchange) {
        String path = getRequest(exchange).getURI().getPath();
        return path == null ? "" : path;
    }

    /**
     * 获取完整请求 URL，不包含查询参数。
     *
     * @param exchange 当前 exchange
     * @return 请求 URL
     */
    public static String getRequestUrl(ServerWebExchange exchange) {
        URI uri = getRequest(exchange).getURI();
        String value = uri.toString();
        int queryIndex = value.indexOf('?');
        return queryIndex >= 0 ? value.substring(0, queryIndex) : value;
    }

    /**
     * 获取 HTTP 方法名称。
     *
     * @param exchange 当前 exchange
     * @return HTTP 方法名称
     */
    public static String getRequestMethod(ServerWebExchange exchange) {
        return getRequest(exchange).getMethodValue();
    }

    /**
     * 获取原始查询字符串。
     *
     * @param exchange 当前 exchange
     * @return 查询字符串，不存在时返回 {@code null}
     */
    public static String getRequestQueryString(ServerWebExchange exchange) {
        return getRequest(exchange).getURI().getRawQuery();
    }

    /**
     * 获取查询参数值。WebFlux 请求体参数需要通过 {@link #getFormData(ServerWebExchange)} 异步读取。
     *
     * @param exchange 当前 exchange
     * @param name     参数名称
     * @return 第一个查询参数值
     */
    public static String getRequestParameter(ServerWebExchange exchange, String name) {
        return getRequest(exchange).getQueryParams().getFirst(name);
    }

    /**
     * 获取查询参数。
     *
     * @param exchange 当前 exchange
     * @return 查询参数
     */
    public static MultiValueMap<String, String> getRequestParameterMap(ServerWebExchange exchange) {
        return getRequest(exchange).getQueryParams();
    }

    /**
     * 异步读取表单参数。
     *
     * @param exchange 当前 exchange
     * @return 表单参数响应式结果
     */
    public static Mono<MultiValueMap<String, String>> getFormData(ServerWebExchange exchange) {
        return requireExchange(exchange).getFormData();
    }

    /**
     * 获取请求体数据流。调用方负责在消费后正确处理 DataBuffer。
     *
     * @param exchange 当前 exchange
     * @return 请求体数据流
     */
    public static Flux<DataBuffer> getRequestBody(ServerWebExchange exchange) {
        return getRequest(exchange).getBody();
    }

    /**
     * 兼容 Servlet 版工具类的输入流命名；WebFlux 返回非阻塞的 DataBuffer 流，而不是阻塞 InputStream。
     *
     * @param exchange 当前 exchange
     * @return 请求体数据流
     */
    public static Flux<DataBuffer> getRequestInputStream(ServerWebExchange exchange) {
        return getRequestBody(exchange);
    }

    /**
     * 获取请求内容类型。
     *
     * @param exchange 当前 exchange
     * @return Content-Type 字符串
     */
    public static String getRequestContentType(ServerWebExchange exchange) {
        MediaType contentType = getRequest(exchange).getHeaders().getContentType();
        return contentType == null ? null : contentType.toString();
    }

    /**
     * 获取请求体长度。
     *
     * @param exchange 当前 exchange
     * @return 字节长度，未知时为 {@code -1}
     */
    public static long getRequestContentLength(ServerWebExchange exchange) {
        return getRequest(exchange).getHeaders().getContentLength();
    }

    /**
     * 获取客户端 IP。信任代理头的前提与 Servlet 版相同：入口代理必须清洗或覆盖转发头。
     *
     * @param exchange 当前 exchange
     * @return 客户端 IP
     */
    public static String getRequestIp(ServerWebExchange exchange) {
        ServerHttpRequest request = getRequest(exchange);
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String remoteAddr = remoteAddress == null ? null : remoteAddress.getHostString();
        return WebUtilsSupport.resolveClientIp(request.getHeaders()::getFirst, remoteAddr);
    }

    /**
     * 获取请求协议。
     *
     * @param exchange 当前 exchange
     * @return 协议名称
     */
    public static String getRequestScheme(ServerWebExchange exchange) {
        return getRequest(exchange).getURI().getScheme();
    }

    /**
     * 获取服务器主机名。
     *
     * @param exchange 当前 exchange
     * @return 主机名，无法解析时为空字符串
     */
    public static String getRequestServerName(ServerWebExchange exchange) {
        String host = getRequest(exchange).getURI().getHost();
        return host == null ? "" : host;
    }

    /**
     * 获取服务器端口。
     *
     * @param exchange 当前 exchange
     * @return 端口，URI 未包含端口时为 {@code -1}
     */
    public static int getRequestServerPort(ServerWebExchange exchange) {
        return getRequest(exchange).getURI().getPort();
    }

    /**
     * 获取指定请求头。
     *
     * @param exchange 当前 exchange
     * @param name     请求头名称
     * @return 请求头值
     */
    public static String getRequestHeader(ServerWebExchange exchange, String name) {
        return getRequest(exchange).getHeaders().getFirst(name);
    }

    /**
     * 获取 WebFlux 最佳匹配的路由模板，没有模板时回退到 URI 路径。
     *
     * @param exchange 当前 exchange
     * @return 路由模板或请求路径
     */
    public static String getRequestPath(ServerWebExchange exchange) {
        Object pattern = requireExchange(exchange).getAttribute(
                org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? getRequestURI(exchange) : String.valueOf(pattern);
    }

    /**
     * 设置响应头。
     *
     * @param exchange 当前 exchange
     * @param name     响应头名称
     * @param value    响应头值
     */
    public static void setResponseHeader(ServerWebExchange exchange, String name, String value) {
        getResponse(exchange).getHeaders().set(name, value);
    }

    /**
     * 设置响应状态码。
     *
     * @param exchange 当前 exchange
     * @param status   HTTP 状态码
     */
    public static void setResponseStatus(ServerWebExchange exchange, int status) {
        getResponse(exchange).setRawStatusCode(status);
    }

    /**
     * 设置响应内容类型。
     *
     * @param exchange 当前 exchange
     * @param type     内容类型
     */
    public static void setResponseContentType(ServerWebExchange exchange, String type) {
        getResponse(exchange).getHeaders().setContentType(MediaType.parseMediaType(type));
    }

    /**
     * 配置文件下载响应头。响应内容需由调用方继续通过 {@code writeWith} 写入。
     *
     * @param exchange 当前 exchange
     * @param fileName 文件名
     * @param suffix   文件后缀
     * @param mimeType MIME 类型
     */
    public static void setDownloadHeader(ServerWebExchange exchange, String fileName, String suffix,
            String mimeType) {
        ServerHttpResponse response = getResponse(exchange);
        HttpHeaders headers = response.getHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.set("Access-Control-Expose-Headers", "Content-Disposition");
        headers.set("Content-Disposition", WebUtilsSupport.buildContentDisposition(fileName, suffix));
    }

    /**
     * 写入 JSON 响应。
     *
     * @param exchange 当前 exchange
     * @param json     JSON 文本
     * @return 写入完成信号
     */
    public static Mono<Void> writeResponseJson(ServerWebExchange exchange, String json) {
        return writeResponse(exchange, json == null ? "null" : json, JSON_UTF8);
    }

    /**
     * 写入文本响应。
     *
     * @param exchange 当前 exchange
     * @param text     文本内容
     * @return 写入完成信号
     */
    public static Mono<Void> writeResponseText(ServerWebExchange exchange, String text) {
        return writeResponse(exchange, text == null ? "" : text, TEXT_UTF8);
    }

    /**
     * 写入 XML 响应。
     *
     * @param exchange 当前 exchange
     * @param xml      XML 内容
     * @return 写入完成信号
     */
    public static Mono<Void> writeResponseXml(ServerWebExchange exchange, String xml) {
        return writeResponse(exchange, xml == null ? "" : xml, XML_UTF8);
    }

    /**
     * 写入二进制响应。
     *
     * @param exchange 当前 exchange
     * @param contentType 内容类型
     * @param bytes    二进制内容
     * @return 写入完成信号
     */
    public static Mono<Void> writeResponseBytes(ServerWebExchange exchange, String contentType, byte[] bytes) {
        return writeResponse(exchange, bytes == null ? new byte[0] : bytes,
                MediaType.parseMediaType(contentType));
    }

    private static Mono<Void> writeResponse(ServerWebExchange exchange, String content, MediaType mediaType) {
        return writeResponse(exchange, content.getBytes(StandardCharsets.UTF_8), mediaType);
    }

    private static Mono<Void> writeResponse(ServerWebExchange exchange, byte[] bytes, MediaType mediaType) {
        ServerHttpResponse response = getResponse(exchange);
        response.getHeaders().setContentType(mediaType);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static ServerWebExchange requireExchange(ServerWebExchange exchange) {
        if (exchange == null) {
            throw new IllegalStateException("无法获取请求上下文。此方法必须在 WebFlux 请求上下文中调用。");
        }
        return exchange;
    }
}
