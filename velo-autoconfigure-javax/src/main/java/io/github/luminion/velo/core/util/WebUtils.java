package io.github.luminion.velo.core.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author luminion
 */
public abstract class WebUtils {

    /**
     * 判断当前是否在Web请求上下文中。
     *
     * @return 如果当前请求是Web请求上下文，则返回true，否则返回false
     */
    public static boolean isWebContext() {
        return RequestContextHolder.getRequestAttributes() != null && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes;
    }


    /**
     * 获取当前请求的属性对象。
     *
     * @return ServletRequestAttributes对象
     * @throws IllegalStateException 如果当前不在一个web请求上下文中
     */
    public static ServletRequestAttributes getServletRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("无法获取请求属性。此方法必须在Spring Web请求上下文中调用。");
        }
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 获取当前请求的ServletContext对象。
     *
     * @return ServletContext对象
     */
    public static ServletContext getServletContext() {
        return getRequest().getServletContext();
    }

    /**
     * 获取Web应用程序的上下文路径。
     *
     * @return 上下文路径
     */
    public static String getServletContextPath() {
        return getServletContext().getContextPath();
    }

    /**
     * 获取当前请求的HttpServletRequest对象。
     *
     * @return HttpServletRequest对象
     * @throws IllegalStateException 如果当前请求对象不可用
     */
    public static HttpServletRequest getRequest() {
        HttpServletRequest request = getServletRequestAttributes().getRequest();
        return request;
    }

    /**
     * 获取当前请求的HttpServletResponse对象。
     *
     * @return HttpServletResponse对象
     * @throws IllegalStateException 如果当前响应对象不可用
     */
    public static HttpServletResponse getResponse() {
        HttpServletResponse response = getServletRequestAttributes().getResponse();
        if (response == null) {
            throw new IllegalStateException("无法获取HttpServletResponse。当前上下文中响应对象不可用。");
        }
        return response;
    }

    /**
     * 设置文件下载的 Response Header (通用核心方法)
     * 处理了各种浏览器的中文乱码问题和空格变+号问题
     *
     * @param fileName 文件名（不含后缀，或包含都可以，具体看调用方）
     * @param suffix   文件后缀 (如 .xlsx, .pdf)
     * @param mimeType 文件的MIME类型
     */
    public static void setDownloadHeader(String fileName, String suffix, String mimeType) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = String.valueOf(System.currentTimeMillis());
        }
        if (suffix != null && !suffix.isEmpty() && !fileName.endsWith(suffix)) {
            fileName += suffix;
        }

        HttpServletResponse response = getResponse();
        response.setContentType(mimeType);
        response.setCharacterEncoding("UTF-8");

        try {
            // URL编码，并将空格产生的 '+' 替换为标准的 '%20'
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            // 兼容现代浏览器和老版IE的标准写法 (RFC 5987)
            String contentDisposition = String.format("attachment; filename=\"%s\"; filename*=utf-8''%s",
                    encodedFileName, encodedFileName);

            // 解决前端跨域获取不到 header 的问题
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setHeader("Content-Disposition", contentDisposition);
        } catch (Exception e) {
            throw new RuntimeException("设置文件下载响应头失败", e);
        }
    }


    /**
     * 获取 Excel (xlsx) 输出流
     */
    public static OutputStream getExcelOutputStream(String fileName) {
        setDownloadHeader(fileName, ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return getResponseOutputStream();
    }

    /**
     * 获取 CSV 输出流
     */
    public static OutputStream getCsvOutputStream(String fileName) {
        setDownloadHeader(fileName, ".csv", "text/csv");
        return getResponseOutputStream();
    }

    /**
     * 获取 Word (docx) 输出流
     */
    public static OutputStream getWordOutputStream(String fileName) {
        setDownloadHeader(fileName, ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return getResponseOutputStream();
    }

    /**
     * 获取 PDF 输出流 (作为附件下载)
     */
    public static OutputStream getPdfOutputStream(String fileName) {
        setDownloadHeader(fileName, ".pdf", "application/pdf");
        return getResponseOutputStream();
    }

    /**
     * 获取 ZIP 压缩包输出流
     */
    public static OutputStream getZipOutputStream(String fileName) {
        setDownloadHeader(fileName, ".zip", "application/zip");
        return getResponseOutputStream();
    }

    /**
     * 获取通用二进制流 (不知道具体文件类型时)
     */
    public static OutputStream getBinaryOutputStream(String fileName) {
        setDownloadHeader(fileName, "", "application/octet-stream");
        return getResponseOutputStream();
    }
    

    /**
     * 获取当前请求的HttpSession对象。
     *
     * @return HttpSession对象
     */
    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    /**
     * 获取请求的URI路径部分。
     * <p>
     * 返回此请求的URL中从协议名称之后到查询字符串之前的部分。
     *
     * @return 请求的URI字符串
     */
    public static String getRequestURI() {
        return getRequest().getRequestURI();
    }

    /**
     * 获取客户端用于发起请求的完整URL（不含查询参数）。
     *
     * @return 包含协议、服务器名、端口号和服务器路径的URL字符串
     */
    public static String getRequestUrl() {
        return getRequest().getRequestURL().toString();
    }

    /**
     * 获取请求的HTTP方法名称 (GET, POST, etc.)。
     *
     * @return HTTP方法名
     */
    public static String getRequestMethod() {
        return getRequest().getMethod();
    }

    /**
     * 获取请求的查询字符串。
     *
     * @return URL中 '?' 之后的部分，如果不存在则为 null
     */
    public static String getRequestQueryString() {
        return getRequest().getQueryString();
    }

    /**
     * 获取请求的内容类型 (Content-Type)。
     *
     * @return 请求的内容类型
     */
    public static String getRequestContentType() {
        return getRequest().getContentType();
    }

    /**
     * 获取请求体的长度（以字节为单位）。
     *
     * @return 请求体的长度
     */
    public static int getRequestContentLength() {
        return getRequest().getContentLength();
    }

    /**
     * 获取客户端真实IP地址。
     * <p>
     * 此方法会尝试从常见的HTTP代理头中获取真实的客户端IP地址，如 'x-forwarded-for', 'Proxy-Client-IP', 'WL-Proxy-Client-IP', 'X-Real-IP'等。
     * 如果无法从代理头中获取，则返回直接连接的客户端IP地址。
     *
     * @return 客户端IP地址
     */
    public static String getRequestIp() {
        HttpServletRequest request = getRequest();
        String ip = null;
        String[] headers = {"x-forwarded-for", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"};
        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 转换localhost的IPv6地址为IPv4格式
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // 处理多级代理，获取第一个有效IP
        if (ip != null && ip.indexOf(',') > 0) {
            final String[] ips = ip.trim().split(",");
            for (String subIp : ips) {
                String trimmedIp = subIp.trim();
                if (!trimmedIp.isEmpty() && !"unknown".equalsIgnoreCase(trimmedIp)) {
                    ip = trimmedIp;
                    break;
                }
            }
        }
        return ip;
    }

    /**
     * 获取请求的协议 (http, https)。
     *
     * @return 请求的协议
     */
    public static String getRequestScheme() {
        return getRequest().getScheme();
    }

    /**
     * 获取服务器的主机名。
     *
     * @return 服务器的主机名
     */
    public static String getRequestServerName() {
        return getRequest().getServerName();
    }

    /**
     * 获取服务器的端口号。
     *
     * @return 服务器的端口号
     */
    public static int getRequestServerPort() {
        return getRequest().getServerPort();
    }

    /**
     * 获取指定的请求头信息。
     *
     * @param key 请求头的键
     * @return 对应键的请求头值
     */
    public static String getRequestHeader(String key) {
        return getRequest().getHeader(key);
    }

    /**
     * 获取指定的请求参数值。
     *
     * @param name 参数名称
     * @return 参数值
     */
    public static String getRequestParameter(String name) {
        return getRequest().getParameter(name);
    }

    /**
     * 获取所有请求参数。
     *
     * @return 包含所有请求参数的Map
     */
    public static Map<String, String[]> getRequestParameterMap() {
        return getRequest().getParameterMap();
    }

    /**
     * 获取请求的输入流。
     *
     * @return 请求的输入流
     * @throws UncheckedIOException 如果发生I/O错误
     */
    public static InputStream getRequestInputStream() {
        try {
            return getRequest().getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("获取请求输入流失败", e);
        }
    }

    /**
     * 设置响应头。
     *
     * @param name  响应头的名称
     * @param value 响应头的值
     */
    public static void setResponseHeader(String name, String value) {
        getResponse().setHeader(name, value);
    }

    /**
     * 设置响应状态码。
     *
     * @param sc 状态码
     */
    public static void setResponseStatus(int sc) {
        getResponse().setStatus(sc);
    }

    /**
     * 设置响应的内容类型。
     *
     * @param type 内容类型
     */
    public static void setResponseContentType(String type) {
        getResponse().setContentType(type);
    }

    /**
     * 获取用于向客户端发送二进制数据的ServletOutputStream对象。
     * <b>注意：</b>不应手动关闭此流，Servlet容器会负责管理它的生命周期。
     *
     * @return OutputStream
     * @throws UncheckedIOException 如果发生I/O错误
     */
    public static OutputStream getResponseOutputStream() {
        try {
            return getResponse().getOutputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("获取响应输出流失败", e);
        }
    }

    /**
     * 获取用于向客户端发送字符文本的PrintWriter对象。
     * <b>注意：</b>不应手动关闭此Writer，Servlet容器会负责管理它的生命周期。
     *
     * @return PrintWriter对象
     * @throws UncheckedIOException 如果发生I/O错误
     */
    public static PrintWriter getResponseWriter() {
        try {
            return getResponse().getWriter();
        } catch (IOException e) {
            throw new UncheckedIOException("获取响应PrintWriter失败", e);
        }
    }

    /**
     * 将JSON字符串写入响应。此方法会设置ContentType为application/json;charset=UTF-8。
     *
     * @param jsonString 要写入的JSON字符串
     */
    public static void writeResponseJson(String jsonString) {
        setResponseContentType("application/json;charset=UTF-8");
        getResponseWriter().write(jsonString);
    }

    /**
     * 将文本字符串写入响应。此方法会设置ContentType为text/plain;charset=UTF-8。
     *
     * @param textString 要写入的文本字符串
     */
    public static void writeResponseText(String textString) {
        setResponseContentType("text/plain;charset=UTF-8");
        getResponseWriter().write(textString);
    }

    /**
     * 将XML字符串写入响应。此方法会设置ContentType为application/xml;charset=UTF-8。
     *
     * @param xmlString 要写入的XML字符串
     */
    public static void writeResponseXml(String xmlString) {
        setResponseContentType("application/xml;charset=UTF-8");
        getResponseWriter().write(xmlString);
    }
}
