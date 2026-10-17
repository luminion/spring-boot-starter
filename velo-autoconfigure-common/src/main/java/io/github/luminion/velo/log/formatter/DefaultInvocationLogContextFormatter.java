package io.github.luminion.velo.log.formatter;

import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.spi.InvocationLogContextFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.StringJoiner;

/**
 * 已知公共字段的默认文本格式；不可用的字段省略。
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
        add(values, "ip", context.getClientIp());
        add(values, "client", context.getClientName());
        add(values, "contextId", context.getContextId());
        add(values, "method", context.getHttpMethod());
        add(values, "route", context.getPath());
        add(values, "instance", context.getInstanceAddress());
        return values.toString();
    }

    private void add(StringJoiner values, String name, String value) {
        if (value != null && !value.isEmpty()) {
            boolean quote = value.chars().anyMatch(Character::isWhitespace);
            String text = quote ? InvocationLogSupport.quote(value) : value;
            values.add(name + "=" + text);
        }
    }
}
