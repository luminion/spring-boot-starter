package io.github.luminion.starter.log.support;

import io.github.luminion.starter.log.LogWriter;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.stream.IntStream;

/**
 * @author luminion
 * @since 1.0.0
 */
public abstract class AbstractLogWriter implements LogWriter {

    protected String getMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    protected String getFormatArgs(String[] paramNames, Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        IntStream.range(0, args.length).forEach(i -> {
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            sb.append(paramName).append("=").append(args[i]);
            if (i < args.length - 1) {
                sb.append(", ");
            }
        });
        sb.append("]");
        return sb.toString();
    }
}
