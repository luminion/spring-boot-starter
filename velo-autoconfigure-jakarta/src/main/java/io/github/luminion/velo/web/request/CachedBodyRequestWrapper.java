package io.github.luminion.velo.web.request;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

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

    @Override
    public String getContentType() {
        String contentType = super.getContentType();
        if (contentType != null) {
            return contentType;
        }
        // 某些包装链路下 getContentType() 会丢值，这里退回到原始请求头读取。
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

    static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream bais;
        @SuppressWarnings("unused")
        private ReadListener readListener;

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
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            this.readListener = readListener;
            // 请求体已经完整缓存到内存，可以立即同步触发非阻塞回调。
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

