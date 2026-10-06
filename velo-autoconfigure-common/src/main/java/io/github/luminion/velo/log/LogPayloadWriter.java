package io.github.luminion.velo.log;

import java.io.IOException;
import java.io.Writer;

/** 本次日志调用的有限字符缓冲；超限信号使原生序列化及时停止。 */
final class LogPayloadWriter extends Writer {
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private final int maxLength;
    private final int previewLimit;
    private final StringBuilder buffer;
    private final LimitReached limitReached = new LimitReached();
    private int previewEnd;
    private boolean truncated;

    LogPayloadWriter(int maxLength) {
        this.maxLength = maxLength;
        this.previewLimit = maxLength > 3 ? maxLength - 3 : maxLength;
        this.buffer = new StringBuilder(maxLength < 0 ? 256 : Math.min(maxLength, 4096));
    }

    @Override
    public void write(char[] value, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > value.length - length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = offset; i < offset + length; i++) {
            appendCharacter(value[i]);
        }
    }

    @Override
    public void write(String value, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > value.length() - length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = offset; i < offset + length; i++) {
            appendCharacter(value.charAt(i));
        }
    }

    @Override
    public void write(int value) throws IOException {
        appendCharacter((char) value);
    }

    private void appendCharacter(char value) throws IOException {
        boolean shortEscape = value == '\n' || value == '\r' || value == '\t';
        boolean unicodeEscape = !shortEscape && (value < 0x20 || value == '\u2028' || value == '\u2029');
        int length = shortEscape ? 2 : 1;
        if (unicodeEscape) {
            length = 6;
        }
        if (truncated || maxLength >= 0 && length > maxLength - buffer.length()) {
            truncated = true;
            throw limitReached;
        }
        if (shortEscape) {
            buffer.append('\\');
            if (value == '\n') {
                buffer.append('n');
            } else if (value == '\r') {
                buffer.append('r');
            } else {
                buffer.append('t');
            }
        } else if (unicodeEscape) {
            buffer.append("\\u");
            buffer.append(HEX[value >> 12 & 0xf]);
            buffer.append(HEX[value >> 8 & 0xf]);
            buffer.append(HEX[value >> 4 & 0xf]);
            buffer.append(HEX[value & 0xf]);
        } else {
            buffer.append(value);
        }
        // 记录可用预览边界，避免截断自己生成的转义或 UTF-16 代理对。
        if (buffer.length() <= previewLimit && !Character.isHighSurrogate(value)) {
            previewEnd = buffer.length();
        }
    }

    boolean isLimitReached(Throwable error) {
        for (int depth = 0; error != null && depth < 64; depth++) {
            if (error == limitReached) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    String content(boolean omittedValues) {
        if (!truncated && !omittedValues) {
            return buffer.toString();
        }
        if (maxLength < 0) {
            return buffer.toString() + "...";
        }
        return buffer.substring(0, previewEnd) + (maxLength > 3 ? "..." : "");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    private static final class LimitReached extends IOException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
