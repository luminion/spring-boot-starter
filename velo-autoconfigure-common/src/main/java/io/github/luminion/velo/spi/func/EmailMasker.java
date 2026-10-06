package io.github.luminion.velo.spi.func;

import java.util.function.Function;

/**
 * 邮箱脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
public class EmailMasker implements Function<String, String> {

    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }
        int separator = s.lastIndexOf('@');
        if (separator <= 0 || separator == s.length() - 1) {
            return "****";
        }
        int prefixEnd = s.offsetByCodePoints(0, 1);
        return s.substring(0, prefixEnd) + "****" + s.substring(separator);
    }

}
