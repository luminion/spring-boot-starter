package io.github.luminion.velo.spi.func;

import java.util.function.Function;

/**
 * 身份证号脱敏处理器
 *
 * @author luminion
 * @since 1.0.0
 */
public class IdCardMasker implements Function<String, String> {

    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("(\\d{6})\\d{9}(\\w{3})", "$1*********$2");
    }

}
