package io.github.luminion.velo.core.spi.func;

import java.util.function.Function;

/**
 * 手机号脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
public class PhoneMasker implements Function<String, String> {

    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

}
