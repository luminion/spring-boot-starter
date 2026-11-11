package io.github.luminion.velo.log.formatter;

import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.spi.InvocationLogContextFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.StringJoiner;

/**
 * 公共块的默认文本格式，字段不带标签、以空格分隔，不可用的字段省略：
 * <ul>
 *     <li>Controller：{@code 调用方IP HTTP方法 路径}，例如 {@code 127.0.0.1 GET /orders/{id}}；</li>
 *     <li>Feign：{@code HTTP方法 路径}，例如 {@code GET /users/{id}}，入口和出口日志一致，按路径即可定位；</li>
 *     <li>普通方法和任务入口没有公共块。</li>
 * </ul>
 * Feign 的客户端名称、contextId 和实际实例地址仍保存在 {@link InvocationLogContext} 中，默认不输出；
 * 需要时可通过自定义 {@link InvocationLogContextFormatter} 追加。
 */
public final class DefaultInvocationLogContextFormatter implements InvocationLogContextFormatter {
    private final InvocationLogSource source;

    public DefaultInvocationLogContextFormatter(InvocationLogSource source) {
        this.source = source;
    }

    @Override
    public InvocationLogSource getSource() {
        return source;
    }

    @Override
    public String format(LogInvocation invocation, InvocationLogContext context) {
        if (context == null) {
            return "";
        }
        StringJoiner values = new StringJoiner(" ");
        add(values, context.getClientIp());
        add(values, context.getHttpMethod());
        add(values, context.getPath());
        return values.toString();
    }

    private void add(StringJoiner values, String value) {
        if (value != null && !value.isEmpty()) {
            boolean quote = value.chars().anyMatch(Character::isWhitespace);
            values.add(quote ? InvocationLogSupport.quote(value) : value);
        }
    }
}
