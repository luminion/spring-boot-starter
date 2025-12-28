package io.github.luminion.autoconfigure.jakarta.request;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求包装器
 *
 * @author luminion
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.body = toByteArray(request.getInputStream());
    }

    public CachedBodyRequestWrapper(HttpServletRequest request, byte[] providedBody) {
        super(request);
        this.body = providedBody != null ? providedBody : new byte[0];
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String enc = getCharacterEncoding();
        if (enc == null) enc = StandardCharsets.UTF_8.name();
        return new BufferedReader(new InputStreamReader(getInputStream(), enc));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /**
     * 重写getContentType方法，确保能从原始请求中获取ContentType
     * <p>
     * 修复问题：在使用CachedBodyRequestWrapper包装请求后，某些框架可能无法正确获取ContentType
     * 这里显式地从原始请求中获取ContentType并返回
     *
     * @return ContentType
     */
    @Override
    public String getContentType() {
        // 从原始请求中获取ContentType，确保包装后的请求也能正确获取
        String contentType = super.getContentType();
        if (contentType != null) {
            return contentType;
        }
        // 如果原始请求没有ContentType，尝试从Header中获取
        return getHeader("Content-Type");
    }

    private static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    // 内部类：基于内存的 ServletInputStream
    static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream bais;
        @SuppressWarnings("unused")
        private ReadListener readListener; // 用于异步读取支持，虽然这里同步实现但需要保留接口兼容性

        CachedBodyServletInputStream(byte[] body) {
            this.bais = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return bais.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return bais.read(b, off, len);
        }

        @Override
        public int available() {
            return bais.available();
        }

        @Override
        public boolean isFinished() {
            return bais.available() == 0;
        }

        @Override
        public boolean isReady() {
            // 内存数据随时可读
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            this.readListener = readListener;
            // 简化的非阻塞支持：数据已在内存，立即通知
            try {
                if (!isFinished()) {
                    readListener.onDataAvailable();
                }
                if (isFinished()) {
                    readListener.onAllDataRead();
                }
            } catch (IOException e) {
                readListener.onError(e);
            }
        }
    }
}